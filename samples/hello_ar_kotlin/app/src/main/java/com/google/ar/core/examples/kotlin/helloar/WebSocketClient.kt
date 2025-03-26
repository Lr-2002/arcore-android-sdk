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

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
        if (isConnected) return

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, webSocketListener)
        Log.d(TAG, "Connecting to WebSocket server at $serverUrl")
    }

    /**
     * Disconnect from the WebSocket server.
     */
    fun disconnect() {
        cancelReconnect()
        webSocket?.close(1000, "Closing connection")
        webSocket = null
        isConnected = false
    }

    /**
     * Send pose data to the server.
     */
    fun sendPoseData(pose: PoseTracker.RelativePose) {
        if (!isConnected) {
            connect()
            return
        }

        try {
            val jsonObject = JSONObject().apply {
                // Position data
                put("x", pose.x)
                put("y", pose.y)
                put("z", pose.z)
                
                // Rotation data (in degrees)
                put("roll", pose.roll)
                put("pitch", pose.pitch)
                put("yaw", pose.yaw)
                
                // Scale
                put("scale", pose.scale)
                
                // Timestamp
                put("timestamp", System.currentTimeMillis())
            }

            webSocket?.send(jsonObject.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending pose data: ${e.message}")
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
