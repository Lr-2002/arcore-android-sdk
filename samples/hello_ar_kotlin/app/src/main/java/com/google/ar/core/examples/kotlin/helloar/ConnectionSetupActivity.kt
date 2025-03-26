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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Activity for setting up WebSocket connection parameters before starting the AR experience.
 */
class ConnectionSetupActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ConnectionSetupActivity"
        
        // Keys for storing connection parameters
        const val EXTRA_IP_ADDRESS = "ip_address"
        const val EXTRA_PORT_NUMBER = "port_number"
        
        // Default values
        const val DEFAULT_IP_ADDRESS = "10.1.123.244"
        const val DEFAULT_PORT_NUMBER = "9999"
    }
    
    private lateinit var ipAddressEditText: TextInputEditText
    private lateinit var portNumberEditText: TextInputEditText
    private lateinit var startButton: MaterialButton
    
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
            if (validateInputs()) {
                startArExperience()
            }
        }
    }
    
    /**
     * Validates user inputs for IP address and port number.
     */
    private fun validateInputs(): Boolean {
        val ipAddress = ipAddressEditText.text.toString().trim()
        val portNumber = portNumberEditText.text.toString().trim()
        
        // Check if IP address is valid
        if (TextUtils.isEmpty(ipAddress)) {
            ipAddressEditText.error = "IP address cannot be empty"
            return false
        }
        
        // Check if port number is valid
        if (TextUtils.isEmpty(portNumber)) {
            portNumberEditText.error = "Port number cannot be empty"
            return false
        }
        
        try {
            val port = portNumber.toInt()
            if (port <= 0 || port > 65535) {
                portNumberEditText.error = "Port number must be between 1 and 65535"
                return false
            }
        } catch (e: NumberFormatException) {
            portNumberEditText.error = "Invalid port number"
            return false
        }
        
        return true
    }
    
    /**
     * Starts the AR experience activity with the connection parameters.
     */
    private fun startArExperience() {
        val ipAddress = ipAddressEditText.text.toString().trim()
        val portNumber = portNumberEditText.text.toString().trim()
        
        Log.d(TAG, "Starting AR experience with IP: $ipAddress, Port: $portNumber")
        
        // Create intent with connection parameters
        val intent = Intent(this, HelloArActivity::class.java).apply {
            putExtra(EXTRA_IP_ADDRESS, ipAddress)
            putExtra(EXTRA_PORT_NUMBER, portNumber)
        }
        
        // Start the AR activity
        startActivity(intent)
    }
}
