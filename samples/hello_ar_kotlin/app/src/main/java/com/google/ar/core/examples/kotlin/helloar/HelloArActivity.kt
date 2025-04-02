/*
 * Copyright 2021 Google LLC
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

import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.ar.core.Config
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.Session
import com.google.ar.core.Pose
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper
import com.google.ar.core.examples.java.common.helpers.DepthSettings
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.exceptions.CameraNotAvailableException
import android.content.Intent

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
class HelloArActivity : AppCompatActivity() {
	companion object {
		private const val TAG = "HelloArActivity"
		private const val POSE_UPDATE_INTERVAL_MS = 100L // 10 updates per second
		private const val EXTRA_SERVER_URL = "server_url"
	}

	// UI elements
	private lateinit var toggleButton: MaterialButton
	private lateinit var actionButton: MaterialButton
	private lateinit var resetPoseButton: MaterialButton
	
	// AR components
	lateinit var renderer: HelloArRenderer
	private lateinit var webSocketClient: WebSocketClient
	private lateinit var poseTracker: PoseTracker
	lateinit var session: Session
	private lateinit var displayRotationHelper: DisplayRotationHelper
	private lateinit var trackingStateHelper: TrackingStateHelper
	lateinit var arView: HelloArView
	
	// Toggle state
	private var isToggleActive = false
	
	// UI update handler
	private val uiHandler = Handler(Looper.getMainLooper())
	private val uiUpdateRunnable = object : Runnable {
		override fun run() {
			updateUIWithPoseData()
			uiHandler.postDelayed(this, 100) // Update every 100ms
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		// Get the server URL from the intent
		val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL) ?: "ws://10.1.123.244:9999"
		
		// Initialize the HelloArView first
		arView = HelloArView(this)
		setContentView(arView.root)
		
		// Initialize AR components
		initializeARComponents(serverUrl)
		
		// Then initialize UI elements that depend on AR components
		initializeUIElements()
		
		// Add the view as a lifecycle observer AFTER initializing AR components
		lifecycle.addObserver(arView)
		
		// Start UI updates
		uiHandler.post(uiUpdateRunnable)
	}
	
	private fun initializeUIElements() {
		try {
			// Position and rotation text views are now handled by HelloArView
			
			// Buttons
			toggleButton = arView.root.findViewById(R.id.toggle_button)
			actionButton = arView.root.findViewById(R.id.action_button)
			resetPoseButton = arView.root.findViewById(R.id.reset_pose_button)
			
			// Set up button click listeners with null checks
			toggleButton?.setOnClickListener {
				isToggleActive = !isToggleActive
				updateToggleButtonAppearance()
				webSocketClient?.sendToggleState(isToggleActive)
			}
			
			actionButton?.setOnClickListener {
				webSocketClient?.sendButtonPress()
			}
			
			resetPoseButton?.setOnClickListener {
				poseTracker?.resetPose()
				webSocketClient?.sendResetPose()
			}
			
			// Initialize the button appearance
			updateToggleButtonAppearance()
		} catch (e: Exception) {
			Log.e(TAG, "Error initializing UI elements: ${e.message}")
			Toast.makeText(this, "Error initializing UI: ${e.message}", Toast.LENGTH_SHORT).show()
		}
	}
	
	private fun initializeARComponents(serverUrl: String) {
		// Create WebSocket client
		webSocketClient = WebSocketClient(serverUrl)
		lifecycle.addObserver(webSocketClient)
		
		// Create PoseTracker
		poseTracker = PoseTracker()
		
		// Create AR session
		try {
			// Create ARCore session that will be used for tracking.
			session = Session(this)
			
			// Create the session config.
			val config = Config(session)
			config.instantPlacementMode = InstantPlacementMode.DISABLED
			session.configure(config)
		} catch (e: UnavailableArcoreNotInstalledException) {
			Toast.makeText(this, "Please install ARCore", Toast.LENGTH_LONG).show()
			finish()
			return
		} catch (e: UnavailableApkTooOldException) {
			Toast.makeText(this, "Please update ARCore", Toast.LENGTH_LONG).show()
			finish()
			return
		} catch (e: UnavailableSdkTooOldException) {
			Toast.makeText(this, "Please update this app", Toast.LENGTH_LONG).show()
			finish()
			return
		} catch (e: UnavailableDeviceNotCompatibleException) {
			Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show()
			finish()
			return
		} catch (e: Exception) {
			Toast.makeText(this, "Failed to create AR session: $e", Toast.LENGTH_LONG).show()
			finish()
			return
		}
		
		// Initialize display rotation helper and tracking state helper
		displayRotationHelper = DisplayRotationHelper(this)
		trackingStateHelper = TrackingStateHelper(this)
		
		// Create the renderer
		renderer = HelloArRenderer(
			this,
			session,
			displayRotationHelper,
			trackingStateHelper,
			webSocketClient,
			poseTracker
		)
		lifecycle.addObserver(renderer)
		
		// Now that we have a renderer, set it on the HelloArView
		// Make sure arView is initialized before calling this
		if (::arView.isInitialized) {
			arView.setRenderer(renderer)
		} else {
			Log.e(TAG, "arView not initialized when trying to set renderer")
		}
	}
	
	private fun updateUIWithPoseData() {
		val relativePose = poseTracker.getCurrentRelativePose()
		
		// Update the UI through HelloArView
		arView.updatePoseDisplay(relativePose)
		
		// Send pose data to server
		webSocketClient?.sendPoseData(
			relativePose.position,
			relativePose.rotation,
			isToggleActive
		)
	}
	
	private fun updateToggleButtonAppearance() {
		try {
			toggleButton?.let { button ->
				if (isToggleActive) {
					button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#14213D")) // Dark blue
					button.setTextColor(Color.WHITE)
					button.text = "Toggle"
				} else {
					button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0F0F0")) // Light gray
					button.setTextColor(Color.BLACK)
					button.text = "UnToggle"
				}
			}
		} catch (e: Exception) {
			Log.e(TAG, "Error updating toggle button: ${e.message}")
		}
	}
	
	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		results: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, results)
		if (!CameraPermissionHelper.hasCameraPermission(this)) {
			Toast.makeText(
				this,
				"Camera permission is needed to run this application",
				Toast.LENGTH_LONG
			).show()
			if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
				// Permission denied with checking "Do not ask again".
				CameraPermissionHelper.launchPermissionSettings(this)
			}
			finish()
		}
	}
	
	override fun onWindowFocusChanged(hasFocus: Boolean) {
		super.onWindowFocusChanged(hasFocus)
		FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus)
	}
	
	override fun onResume() {
		super.onResume()
		
		// Resume the ARCore session
		try {
			session.resume()
		} catch (e: CameraNotAvailableException) {
			Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show()
			finish()
			return
		}
		
		// Start UI updates
		uiHandler.post(uiUpdateRunnable)
	}
	
	override fun onPause() {
		super.onPause()
		
		// Pause the ARCore session
		session.pause()
		
		// Stop UI updates
		uiHandler.removeCallbacks(uiUpdateRunnable)
	}
}
