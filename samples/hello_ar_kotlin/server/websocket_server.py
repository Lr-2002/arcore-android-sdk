#!/usr/bin/env python
"""
WebSocket server that receives position and rotation data from the AR app.
"""
import asyncio
import json
import logging
import websockets
import datetime

# Configure logging
logging.basicConfig(
    format="%(asctime)s %(message)s",
    level=logging.INFO,
)
import socket

def get_local_ip():
    """Get the local IP address of this machine."""
    try:
        # Create a socket that connects to an external server
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        # It doesn't actually connect but helps determine the interface
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"  # Fallback to localhost


# Store connected clients
CLIENTS = set()

async def register(websocket):
    """Register a new client."""
    CLIENTS.add(websocket)
    logging.info(f"Client connected. Total clients: {len(CLIENTS)}")

async def unregister(websocket):
    """Unregister a client."""
    CLIENTS.remove(websocket)
    logging.info(f"Client disconnected. Total clients: {len(CLIENTS)}")

async def handle_connection(websocket):
    """Handle a WebSocket connection."""
    await register(websocket)
    try:
        async for message in websocket:
            try:
                # Parse the JSON message
                data = json.loads(message)
                message_type = data.get('type', 'pose_data')  # Default to pose_data for backward compatibility
                timestamp = datetime.datetime.now()
                formatted_time = timestamp.strftime('%H:%M:%S.%f')[:-3]
                
                if message_type == 'pose_data' or message_type == 'pose':
                    # Handle pose data - support both 'pose_data' and 'pose' message types
                    if message_type == 'pose':
                        # New format with nested position and rotation objects
                        position_obj = data.get('position', {})
                        rotation_obj = data.get('rotation', {})
                        
                        position = f"Position: X={position_obj.get('x', 0):.3f}, Y={position_obj.get('y', 0):.3f}, Z={position_obj.get('z', 0):.3f}"
                        rotation = f"Rotation: Roll={rotation_obj.get('roll', 0):.2f}°, Pitch={rotation_obj.get('pitch', 0):.2f}°, Yaw={rotation_obj.get('yaw', 0):.2f}°"
                        toggle_state = f"Toggle: {'Active' if data.get('isToggleActive', False) else 'Inactive'}"
                        scale = f"Scale: {data.get('scale', 1.0):.2f}"
                    else:
                        # Old format with flat structure
                        position = f"Position: X={data.get('x', 0):.3f}, Y={data.get('y', 0):.3f}, Z={data.get('z', 0):.3f}"
                        rotation = f"Rotation: Roll={data.get('roll', 0):.2f}°, Pitch={data.get('pitch', 0):.2f}°, Yaw={data.get('yaw', 0):.2f}°"
                        scale = f"Scale: {data.get('scale', 1.0):.2f}"
                        toggle_state = f"Toggle: {'Active' if data.get('toggle_active', False) else 'Inactive'}"
                    
                    logging.info(f"[{formatted_time}] {position} | {rotation} | {scale} | {toggle_state}")
                
                elif message_type == 'toggle_state' or message_type == 'toggle':
                    # Handle toggle state change
                    toggle_active = data.get('toggle_active', data.get('isActive', False))
                    logging.info(f"[{formatted_time}] Toggle state changed: {'Active' if toggle_active else 'Inactive'}")
                
                elif message_type == 'button_press' or message_type == 'button':
                    # Handle button press
                    logging.info(f"[{formatted_time}] Button pressed")
                
                elif message_type == 'reset_pose' or message_type == 'reset':
                    # Handle reset pose command
                    logging.info(f"[{formatted_time}] Pose reset requested")
                
                else:
                    logging.warning(f"[{formatted_time}] Unknown message type: {message_type}")
                
            except json.JSONDecodeError:
                logging.error(f"Failed to parse message: {message}")
            except Exception as e:
                logging.error(f"Error processing message: {e}")
    finally:
        await unregister(websocket)


async def main():
    """Start the WebSocket server."""
    host = "0.0.0.0"  # Listen on all interfaces
    port = 9999
    
    server = await websockets.serve(handle_connection, host, port)
    local_ip = get_local_ip()
    logging.info(f"WebSocket server started at ws://{host}:{port}")
    logging.info(f"Your computer's IP address is {local_ip}")
    logging.info(f"Connect your phone to: ws://{local_ip}:{port}")
    
    # Keep the server running
    await asyncio.Future()
if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logging.info("Server stopped by user")
