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

            val json = JSONObject().apply {
                put("type", "pose")
                put("position", JSONObject().apply {
                    put("x", position[0])
                    put("y", position[1])
                    put("z", position[2])
                })
                put("rotation", JSONObject().apply {
                    put("roll", rotation[0])
                    put("pitch", rotation[1])
                    put("yaw", rotation[2])
                })
                put("isToggleActive", isToggleActive)
            }

            webSocket?.send(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending pose data: ${e.message}")
        }
    }

    /**
     * Sends toggle state to the server.
     */
    fun sendToggleState(isActive: Boolean) {
        try {
            if (!isConnected) return

            val json = JSONObject().apply {
                put("type", "toggle")
                put("isActive", isActive)
            }

            webSocket?.send(json.toString())
            Log.d(TAG, "Sent toggle state: $isActive")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending toggle state: ${e.message}")
        }
    }

    /**
     * Sends button press to the server.
     */
    fun sendButtonPress() {
        try {
            if (!isConnected) return

            val json = JSONObject().apply {
                put("type", "button")
                put("pressed", true)
            }

            webSocket?.send(json.toString())
            Log.d(TAG, "Sent button press")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending button press: ${e.message}")
        }
    }

    /**
     * Sends reset pose command to the server.
     */
    fun sendResetPose() {
        try {
            if (!isConnected) return

            val json = JSONObject().apply {
                put("type", "reset")
            }

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
