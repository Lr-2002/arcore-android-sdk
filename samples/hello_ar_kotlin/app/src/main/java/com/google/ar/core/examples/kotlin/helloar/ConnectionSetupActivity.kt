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

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity for setting up the WebSocket connection to the server.
 */
class ConnectionSetupActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ConnectionSetupActivity"
        
        // Keys for storing connection parameters
        const val EXTRA_SERVER_URL = "server_url"
        
        // Default values
        const val DEFAULT_IP_ADDRESS = "10.1.123.244"
        const val DEFAULT_PORT_NUMBER = "9999"
    }
    
    private lateinit var ipAddressEditText: EditText
    private lateinit var portNumberEditText: EditText
    private lateinit var startButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_setup)
        
        // Initialize UI components
        ipAddressEditText = findViewById(R.id.ipAddressEditText)
        portNumberEditText = findViewById(R.id.portNumberEditText)
        startButton = findViewById(R.id.startButton)
        
        // Set default values
        ipAddressEditText.setText(DEFAULT_IP_ADDRESS)
        portNumberEditText.setText(DEFAULT_PORT_NUMBER)
        
        // Set up button click listener
        startButton.setOnClickListener {
            val ipAddress = ipAddressEditText.text.toString().trim()
            val portNumber = portNumberEditText.text.toString().trim()

            // Validate inputs
            if (ipAddress.isEmpty()) {
                ipAddressEditText.error = "IP address cannot be empty"
                return@setOnClickListener
            }

            if (portNumber.isEmpty()) {
                portNumberEditText.error = "Port number cannot be empty"
                return@setOnClickListener
            }

            // Create WebSocket URL
            val serverUrl = "ws://$ipAddress:$portNumber"

            // Start HelloArActivity with the server URL
            val intent = Intent(this, HelloArActivity::class.java)
            intent.putExtra(EXTRA_SERVER_URL, serverUrl)
            startActivity(intent)
        }
    }
}
