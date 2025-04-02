/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.kotlin.helloar

import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Pose
import okhttp3.*
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * WebSocket client to send pose data to a server.
 */
class WebSocketClient(private val serverUrl: String) : DefaultLifecycleObserver {
    companion object {
        private const val TAG = "WebSocketClient"
        private const val RECONNECT_DELAY = 2000L // 2 seconds
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectJob: Runnable? = null
    private var reconnectHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // Listener for WebSocket events
    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connection opened")
            isConnected = true
            cancelReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            isConnected = false
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $reason")
            isConnected = false
            scheduleReconnect()
        }
    }

    init {
        connect()
    }

    /**
     * Connect to the WebSocket server.
     */
    fun connect() {
        try {
            if (isConnected) return

            val request = Request.Builder()
                .url(serverUrl)
                .build()

            webSocket = client.newWebSocket(request, webSocketListener)
            Log.d(TAG, "Connecting to WebSocket server at $serverUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to WebSocket: ${e.message}")
        }
    }

    /**
     * Disconnect from the WebSocket server.
     */
    fun disconnect() {
        try {
            cancelReconnect()
            webSocket?.close(1000, "Closing connection")
            webSocket = null
            isConnected = false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from WebSocket: ${e.message}")
        }
    }

    /**
     * Sends pose data to the server.
     */
    fun sendPoseData(
        position: FloatArray,
        rotation: FloatArray,
        isToggleActive: Boolean
    ) {
        try {
            if (!isConnected) return

            // Convert Euler angles (in degrees) to rotation matrix
            val rotationMatrix = eulerAnglesToRotationMatrix(rotation)

            val json = JSONObject()
            
            // Create rotation matrix as nested arrays
            val rotationArray = JSONArray()
            
            // First row
            val row0 = JSONArray()
            row0.put(rotationMatrix[0])
            row0.put(rotationMatrix[1])
            row0.put(rotationMatrix[2])
            rotationArray.put(row0)
            
            // Second row
            val row1 = JSONArray()
            row1.put(rotationMatrix[3])
            row1.put(rotationMatrix[4])
            row1.put(rotationMatrix[5])
            rotationArray.put(row1)
            
            // Third row
            val row2 = JSONArray()
            row2.put(rotationMatrix[6])
            row2.put(rotationMatrix[7])
            row2.put(rotationMatrix[8])
            rotationArray.put(row2)
            
            json.put("rotation", rotationArray)
            
            // Add position as an array
            val positionArray = JSONArray()
            positionArray.put(position[0])
            positionArray.put(position[1])
            positionArray.put(position[2])
            json.put("position", positionArray)
            
            // Add button state (false by default)
            json.put("button", false)
            
            // Add toggle state
            json.put("toggle", isToggleActive)

            webSocket?.send(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending pose data: ${e.message}")
        }
    }

    /**
     * Convert Euler angles to rotation matrix.
     * @param eulerAngles Array of [roll, pitch, yaw] in degrees
     * @return 3x3 rotation matrix as a 9-element array [r11, r12, r13, r21, r22, r23, r31, r32, r33]
     */
    private fun eulerAnglesToRotationMatrix(eulerAngles: FloatArray): FloatArray {
        // Convert degrees to radians
        val roll = Math.toRadians(eulerAngles[0].toDouble())
        val pitch = Math.toRadians(eulerAngles[1].toDouble())
        val yaw = Math.toRadians(eulerAngles[2].toDouble())
        
        // Rotation matrix elements
        val cosR = Math.cos(roll)
        val sinR = Math.sin(roll)
        val cosP = Math.cos(pitch)
        val sinP = Math.sin(pitch)
        val cosY = Math.cos(yaw)
        val sinY = Math.sin(yaw)
        
        // Create rotation matrix (ZYX convention)
        val matrix = FloatArray(9)
        
        // First row
        matrix[0] = (cosY * cosP).toFloat()
        matrix[1] = (cosY * sinP * sinR - sinY * cosR).toFloat()
        matrix[2] = (cosY * sinP * cosR + sinY * sinR).toFloat()
        
        // Second row
        matrix[3] = (sinY * cosP).toFloat()
        matrix[4] = (sinY * sinP * sinR + cosY * cosR).toFloat()
        matrix[5] = (sinY * sinP * cosR - cosY * sinR).toFloat()
        
        // Third row
        matrix[6] = (-sinP).toFloat()
        matrix[7] = (cosP * sinR).toFloat()
        matrix[8] = (cosP * cosR).toFloat()
        
        return matrix
    }

    /**
     * Sends button press to the server.
     */
    fun sendButtonPress() {
        try {
            if (!isConnected) return

            val json = JSONObject()
            
            // Create position array
            val positionArray = JSONArray()
            positionArray.put(0)
            positionArray.put(0)
            positionArray.put(0)
            json.put("position", positionArray)
            
            // Create identity rotation matrix
            val rotationArray = JSONArray()
            
            // First row
            val row0 = JSONArray()
            row0.put(1)
            row0.put(0)
            row0.put(0)
            rotationArray.put(row0)
            
            // Second row
            val row1 = JSONArray()
            row1.put(0)
            row1.put(1)
            row1.put(0)
            rotationArray.put(row1)
            
            // Third row
            val row2 = JSONArray()
            row2.put(0)
            row2.put(0)
            row2.put(1)
            rotationArray.put(row2)
            
            json.put("rotation", rotationArray)
            
            // Set button state to true
            json.put("button", true)
            json.put("toggle", false)

            webSocket?.send(json.toString())
            Log.d(TAG, "Sent button press")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending button press: ${e.message}")
        }
    }

    /**
     * Sends toggle state to the server.
     */
    fun sendToggleState(isActive: Boolean) {
        try {
            if (!isConnected) return

            val json = JSONObject()
            
            // Create position array
            val positionArray = JSONArray()
            positionArray.put(0)
            positionArray.put(0)
            positionArray.put(0)
            json.put("position", positionArray)
            
            // Create identity rotation matrix
            val rotationArray = JSONArray()
            
            // First row
            val row0 = JSONArray()
            row0.put(1)
            row0.put(0)
            row0.put(0)
            rotationArray.put(row0)
            
            // Second row
            val row1 = JSONArray()
            row1.put(0)
            row1.put(1)
            row1.put(0)
            rotationArray.put(row1)
            
            // Third row
            val row2 = JSONArray()
            row2.put(0)
            row2.put(0)
            row2.put(1)
            rotationArray.put(row2)
            
            json.put("rotation", rotationArray)
            
            // Set toggle state
            json.put("button", false)
            json.put("toggle", isActive)

            webSocket?.send(json.toString())
            Log.d(TAG, "Sent toggle state: $isActive")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending toggle state: ${e.message}")
        }
    }

    /**
     * Sends reset pose command to the server.
     */
    fun sendResetPose() {
        try {
            if (!isConnected) return

            val json = JSONObject()
            
            // Create position array
            val positionArray = JSONArray()
            positionArray.put(0)
            positionArray.put(0)
            positionArray.put(0)
            json.put("position", positionArray)
            
            // Create identity rotation matrix
            val rotationArray = JSONArray()
            
            // First row
            val row0 = JSONArray()
            row0.put(1)
            row0.put(0)
            row0.put(0)
            rotationArray.put(row0)
            
            // Second row
            val row1 = JSONArray()
            row1.put(0)
            row1.put(1)
            row1.put(0)
            rotationArray.put(row1)
            
            // Third row
            val row2 = JSONArray()
            row2.put(0)
            row2.put(0)
            row2.put(1)
            rotationArray.put(row2)
            
            json.put("rotation", rotationArray)
            
            // Set reset flag
            json.put("button", false)
            json.put("toggle", false)
            json.put("reset", true)

            webSocket?.send(json.toString())
            Log.d(TAG, "Sent reset pose command")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reset pose command: ${e.message}")
        }
    }

    /**
     * Schedule a reconnection attempt.
     */
    private fun scheduleReconnect() {
        cancelReconnect()

        val job = Runnable {
            Log.d(TAG, "Attempting to reconnect to WebSocket server...")
            connect()
        }

        reconnectJob = job
        reconnectHandler.postDelayed(job, RECONNECT_DELAY)
    }

    /**
     * Cancel any pending reconnection attempts.
     */
    private fun cancelReconnect() {
        val job = reconnectJob
        if (job != null) {
            reconnectHandler.removeCallbacks(job)
        }
        reconnectJob = null
    }

    override fun onPause(owner: LifecycleOwner) {
        // Disconnect when the app is paused
        disconnect()
    }

    override fun onResume(owner: LifecycleOwner) {
        // Reconnect when the app is resumed
        connect()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // Clean up resources
        disconnect()
        client.dispatcher.executorService.shutdown()
    }
}
