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

import android.content.res.Resources
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Button
import android.widget.Switch
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.FrameLayout
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Config
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper
import com.google.ar.core.examples.kotlin.helloar.DoubleTapHelper
import com.google.ar.core.examples.java.common.samplerender.SampleRender

/** Contains UI elements for Hello AR. */
class HelloArView(val activity: HelloArActivity) : DefaultLifecycleObserver {
  val root = View.inflate(activity, R.layout.activity_hello_ar, null)
  val surfaceView = GLSurfaceView(activity).apply {
    preserveEGLContextOnPause = true
    setEGLContextClientVersion(2)
    setEGLConfigChooser(8, 8, 8, 8, 16, 0)
  }
  
  // Reference to UI elements from activity_hello_ar.xml
  val posXTextView = root.findViewById<TextView>(R.id.pos_x_value)
  val posYTextView = root.findViewById<TextView>(R.id.pos_y_value)
  val posZTextView = root.findViewById<TextView>(R.id.pos_z_value)
  val rotRollTextView = root.findViewById<TextView>(R.id.rot_roll_value)
  val rotPitchTextView = root.findViewById<TextView>(R.id.rot_pitch_value)
  val rotYawTextView = root.findViewById<TextView>(R.id.rot_yaw_value)
  
  // Add the surfaceView to the AR fragment container
  init {
    val fragmentContainer = root.findViewById<FrameLayout>(R.id.ar_fragment_container)
    fragmentContainer.addView(surfaceView)
  }
  
  // Find settings button safely
  val settingsButton = root.findViewById<ImageButton>(R.id.settings_button)?.apply {
    setOnClickListener { v ->
      PopupMenu(activity, v).apply {
        setOnMenuItemClickListener { item ->
          when (item.itemId) {
            R.id.position_controls -> showPositionControlsDialog()
            R.id.rotation_controls -> showRotationControlsDialog()
            R.id.scale_controls -> showScaleControlsDialog()
            R.id.snapping_controls -> showSnappingControlsDialog()
            else -> null
          } != null
        }
        inflate(R.menu.settings_menu)
        show()
      }
    }
  }

  // Get session from activity directly
  val session
    get() = activity.session

  val snackbarHelper = SnackbarHelper()
  val tapHelper = DoubleTapHelper(activity).also { surfaceView.setOnTouchListener(it) }
  
  // Reference to the renderer to access the PoseTracker
  private var renderer: HelloArRenderer? = null
  
  override fun onResume(owner: LifecycleOwner) {
    // Only call onResume on the surfaceView if it has been properly initialized
    try {
      surfaceView.onResume()
    } catch (e: Exception) {
      // Log the exception but don't crash
      android.util.Log.e("HelloArView", "Error resuming GLSurfaceView: ${e.message}")
    }
    
    // Set up the renderer with the surface view if it's not already set up
    // and if the renderer is available from the activity
    if (renderer == null) {
      try {
        renderer = activity.renderer
        if (renderer != null) {
          SampleRender(surfaceView, renderer!!, activity.assets)
        }
      } catch (e: Exception) {
        // The renderer might not be initialized yet, which is fine
        // We'll set it later when it becomes available
        android.util.Log.d("HelloArView", "Renderer not yet available: ${e.message}")
      }
    }
  }
  
  override fun onPause(owner: LifecycleOwner) {
    try {
      surfaceView.onPause()
    } catch (e: Exception) {
      // Log the exception but don't crash
      android.util.Log.e("HelloArView", "Error pausing GLSurfaceView: ${e.message}")
    }
  }
  
  // Sets the renderer reference to access the PoseTracker
  fun setRenderer(renderer: HelloArRenderer) {
    this.renderer = renderer
    // Initialize the SampleRender here since we now have a valid renderer
    try {
      SampleRender(surfaceView, renderer, activity.assets)
    } catch (e: Exception) {
      android.util.Log.e("HelloArView", "Error initializing SampleRender: ${e.message}")
    }
  }

  /**
   * Updates the pose text view with the current pose information.
   */
  fun updatePoseDisplay(relativePose: PoseTracker.RelativePose) {
    activity.runOnUiThread {
      posXTextView.text = String.format("%.2f", relativePose.x)
      posYTextView.text = String.format("%.2f", relativePose.y)
      posZTextView.text = String.format("%.2f", relativePose.z)
      rotRollTextView.text = String.format("%.2f", relativePose.roll)
      rotPitchTextView.text = String.format("%.2f", relativePose.pitch)
      rotYawTextView.text = String.format("%.2f", relativePose.yaw)
    }
  }

  /**
   * Shows a dialog with position control options.
   */
  private fun showPositionControlsDialog() {
    val poseTracker = renderer?.poseTracker ?: return
    
    val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_position_controls, null)
    val dialog = AlertDialog.Builder(activity)
      .setTitle("Position Controls")
      .setView(dialogView)
      .setPositiveButton("Close", null)
      .create()
    
    // X position controls
    dialogView.findViewById<Button>(R.id.btn_x_minus)?.setOnClickListener {
      poseTracker.translate(-0.05f, 0f, 0f)
    }
    dialogView.findViewById<Button>(R.id.btn_x_plus)?.setOnClickListener {
      poseTracker.translate(0.05f, 0f, 0f)
    }
    
    // Y position controls
    dialogView.findViewById<Button>(R.id.btn_y_minus)?.setOnClickListener {
      poseTracker.translate(0f, -0.05f, 0f)
    }
    dialogView.findViewById<Button>(R.id.btn_y_plus)?.setOnClickListener {
      poseTracker.translate(0f, 0.05f, 0f)
    }
    
    // Z position controls
    dialogView.findViewById<Button>(R.id.btn_z_minus)?.setOnClickListener {
      poseTracker.translate(0f, 0f, -0.05f)
    }
    dialogView.findViewById<Button>(R.id.btn_z_plus)?.setOnClickListener {
      poseTracker.translate(0f, 0f, 0.05f)
    }
    
    // Reset position button
    dialogView.findViewById<Button>(R.id.btn_reset_position)?.setOnClickListener {
      poseTracker.setPosition(0f, 0f, 0f)
      Toast.makeText(activity, "Position reset", Toast.LENGTH_SHORT).show()
    }
    
    dialog.show()
  }
  
  /**
   * Shows a dialog with rotation control options.
   */
  private fun showRotationControlsDialog() {
    val poseTracker = renderer?.poseTracker ?: return
    
    val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_rotation_controls, null)
    val dialog = AlertDialog.Builder(activity)
      .setTitle("Rotation Controls")
      .setView(dialogView)
      .setPositiveButton("Close", null)
      .create()
    
    // Roll controls
    dialogView.findViewById<Button>(R.id.btn_roll_minus)?.setOnClickListener {
      poseTracker.rotate(-15f, 0f, 0f)
    }
    dialogView.findViewById<Button>(R.id.btn_roll_plus)?.setOnClickListener {
      poseTracker.rotate(15f, 0f, 0f)
    }
    
    // Pitch controls
    dialogView.findViewById<Button>(R.id.btn_pitch_minus)?.setOnClickListener {
      poseTracker.rotate(0f, -15f, 0f)
    }
    dialogView.findViewById<Button>(R.id.btn_pitch_plus)?.setOnClickListener {
      poseTracker.rotate(0f, 15f, 0f)
    }
    
    // Yaw controls
    dialogView.findViewById<Button>(R.id.btn_yaw_minus)?.setOnClickListener {
      poseTracker.rotate(0f, 0f, -15f)
    }
    dialogView.findViewById<Button>(R.id.btn_yaw_plus)?.setOnClickListener {
      poseTracker.rotate(0f, 0f, 15f)
    }
    
    // Reset rotation button
    dialogView.findViewById<Button>(R.id.btn_reset_rotation)?.setOnClickListener {
      poseTracker.setRotation(0f, 0f, 0f)
      Toast.makeText(activity, "Rotation reset", Toast.LENGTH_SHORT).show()
    }
    
    dialog.show()
  }
  
  /**
   * Shows a dialog with scale control options.
   */
  private fun showScaleControlsDialog() {
    val poseTracker = renderer?.poseTracker ?: return
    
    val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_scale_controls, null)
    val dialog = AlertDialog.Builder(activity)
      .setTitle("Scale Controls")
      .setView(dialogView)
      .setPositiveButton("Close", null)
      .create()
    
    val scaleSeekBar = dialogView.findViewById<SeekBar>(R.id.scale_seek_bar)
    val scaleValueText = dialogView.findViewById<TextView>(R.id.scale_value_text)
    
    // Initialize the seek bar with the current scale value
    val currentScale = poseTracker.getScale()
    scaleSeekBar?.progress = ((currentScale - 0.1f) * 100f).toInt()
    scaleValueText?.text = String.format("%.2f", currentScale)
    
    // Update scale when seek bar changes
    scaleSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        val newScale = 0.1f + (progress / 100f)
        poseTracker.setScale(newScale)
        scaleValueText?.text = String.format("%.2f", newScale)
      }
      
      override fun onStartTrackingTouch(seekBar: SeekBar?) {}
      
      override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
    
    // Scale decrease button
    dialogView.findViewById<Button>(R.id.btn_scale_down)?.setOnClickListener {
      poseTracker.scale(0.9f)
      val newScale = poseTracker.getScale()
      scaleSeekBar?.progress = ((newScale - 0.1f) * 100f).toInt()
      scaleValueText?.text = String.format("%.2f", newScale)
    }
    
    // Scale increase button
    dialogView.findViewById<Button>(R.id.btn_scale_up)?.setOnClickListener {
      poseTracker.scale(1.1f)
      val newScale = poseTracker.getScale()
      scaleSeekBar?.progress = ((newScale - 0.1f) * 100f).toInt()
      scaleValueText?.text = String.format("%.2f", newScale)
    }
    
    // Reset scale button
    dialogView.findViewById<Button>(R.id.btn_reset_scale)?.setOnClickListener {
      poseTracker.setScale(1.0f)
      scaleSeekBar?.progress = 90 // 1.0 - 0.1 = 0.9 * 100 = 90
      scaleValueText?.text = "1.00"
      Toast.makeText(activity, "Scale reset", Toast.LENGTH_SHORT).show()
    }
    
    dialog.show()
  }
  
  /**
   * Shows a dialog with snapping control options.
   */
  private fun showSnappingControlsDialog() {
    val poseTracker = renderer?.poseTracker ?: return
    
    val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_snapping_controls, null)
    val dialog = AlertDialog.Builder(activity)
      .setTitle("Snapping Controls")
      .setView(dialogView)
      .setPositiveButton("Close", null)
      .create()
    
    // Snapping toggle switch
    val snapSwitch = dialogView.findViewById<Switch>(R.id.switch_snap_enabled)
    snapSwitch?.isChecked = poseTracker.isSnapEnabled()
    snapSwitch?.setOnCheckedChangeListener { _, isChecked ->
      poseTracker.setSnapEnabled(isChecked)
      Toast.makeText(
        activity, 
        if (isChecked) "Snapping enabled" else "Snapping disabled", 
        Toast.LENGTH_SHORT
      ).show()
    }
    
    // Grid size controls
    val gridSizeSeekBar = dialogView.findViewById<SeekBar>(R.id.grid_size_seek_bar)
    val gridSizeText = dialogView.findViewById<TextView>(R.id.grid_size_text)
    
    gridSizeSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        val gridSize = 0.01f + (progress / 100f) * 0.19f // Range from 0.01 to 0.2
        poseTracker.setSnapGridSize(gridSize)
        gridSizeText?.text = String.format("%.2f m", gridSize)
      }
      
      override fun onStartTrackingTouch(seekBar: SeekBar?) {}
      
      override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
    
    // Rotation snap controls
    val rotationSnapSeekBar = dialogView.findViewById<SeekBar>(R.id.rotation_snap_seek_bar)
    val rotationSnapText = dialogView.findViewById<TextView>(R.id.rotation_snap_text)
    
    rotationSnapSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        val snapDegrees = 1f + progress * 0.44f // Range from 1 to 45 degrees
        val snapDegreesRounded = Math.round(snapDegrees)
        poseTracker.setSnapRotationDegrees(snapDegreesRounded.toFloat())
        rotationSnapText?.text = String.format("%d°", snapDegreesRounded)
      }
      
      override fun onStartTrackingTouch(seekBar: SeekBar?) {}
      
      override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
    
    dialog.show()
  }

  /**
   * Shows a pop-up dialog on the first tap in HelloARRenderer, determining whether the user wants
   * to enable depth-based occlusion. The result of this dialog can be retrieved with
   * DepthSettings.useDepthForOcclusion().
   */
  fun showOcclusionDialogIfNeeded() {
    // Simplified to do nothing since we're not using depth features
    return
  }

  private fun launchInstantPlacementSettingsMenuDialog() {
    // Simplified to do nothing since we're not using instant placement
    Toast.makeText(activity, "Instant placement not supported in this version", Toast.LENGTH_SHORT).show()
  }

  /** Shows checkboxes to the user to facilitate toggling of depth-based effects. */
  private fun launchDepthSettingsMenuDialog() {
    // Simplified to do nothing since we're not using depth features
    Toast.makeText(activity, "Depth settings not supported in this version", Toast.LENGTH_SHORT).show()
  }
}
