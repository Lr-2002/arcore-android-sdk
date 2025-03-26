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
                
                # Format and print the received data
                timestamp = datetime.datetime.fromtimestamp(data.get('timestamp', 0) / 1000.0)
                formatted_time = timestamp.strftime('%H:%M:%S.%f')[:-3]
                
                position = f"Position: X={data.get('x', 0):.3f}, Y={data.get('y', 0):.3f}, Z={data.get('z', 0):.3f}"
                rotation = f"Rotation: Roll={data.get('roll', 0):.2f}°, Pitch={data.get('pitch', 0):.2f}°, Yaw={data.get('yaw', 0):.2f}°"
                scale = f"Scale: {data.get('scale', 1.0):.2f}"
                
                logging.info(f"[{formatted_time}] {position} | {rotation} | {scale}")
                
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
    logging.info(f"WebSocket server started at ws://{host}:{port}")
    logging.info(f"Your Mac's IP address is 10.1.123.244")
    
    # Keep the server running
    await asyncio.Future()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logging.info("Server stopped by user")
