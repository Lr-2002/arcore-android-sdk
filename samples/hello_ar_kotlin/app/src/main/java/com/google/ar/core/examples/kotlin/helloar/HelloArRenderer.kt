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

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.LightEstimate
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper
import com.google.ar.core.examples.java.common.samplerender.Framebuffer
import com.google.ar.core.examples.java.common.samplerender.GLError
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.Texture
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.io.IOException

import com.google.ar.core.examples.kotlin.helloar.PoseTracker
import com.google.ar.core.examples.kotlin.helloar.WebSocketClient

/** Renders the HelloAR application using our example Renderer. */
class HelloArRenderer(
    val activity: HelloArActivity,
    val session: Session,
    val displayRotationHelper: DisplayRotationHelper,
    val trackingStateHelper: TrackingStateHelper,
    val webSocketClient: WebSocketClient,
    val poseTracker: PoseTracker
) : DefaultLifecycleObserver, SampleRender.Renderer {
  companion object {
    val TAG = "HelloArRenderer"

    private val Z_NEAR = 0.1f
    private val Z_FAR = 100f
  }

  lateinit var render: SampleRender
  private var backgroundRenderer: BackgroundRenderer? = null
  var hasSetTextureNames = false

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private val modelMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val projectionMatrix = FloatArray(16)
  private val modelViewMatrix = FloatArray(16) // view x model
  private val modelViewProjectionMatrix = FloatArray(16) // projection x view x model

  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
    hasSetTextureNames = false
  }

  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  override fun onSurfaceCreated(render: SampleRender) {
    this.render = render
    
    // Prepare the rendering objects
    try {
      backgroundRenderer = BackgroundRenderer(render)
      // Initialize the shaders for background and occlusion
      backgroundRenderer?.setUseDepthVisualization(render, false)
      backgroundRenderer?.setUseOcclusion(render, false)
    } catch (e: IOException) {
      Log.e(TAG, "Failed to read a required asset file", e)
      showError("Failed to read a required asset file: $e")
    }
  }

  override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
    displayRotationHelper.onSurfaceChanged(width, height)
  }

  override fun onDrawFrame(render: SampleRender) {
    val session = session ?: return
    
    // Check if backgroundRenderer is initialized
    if (backgroundRenderer == null) {
      Log.e(TAG, "backgroundRenderer is null, trying to initialize")
      try {
        backgroundRenderer = BackgroundRenderer(render)
        backgroundRenderer?.setUseDepthVisualization(render, false)
        backgroundRenderer?.setUseOcclusion(render, false)
      } catch (e: IOException) {
        Log.e(TAG, "Failed to initialize backgroundRenderer", e)
        showError("Failed to initialize renderer: $e")
        return
      }
    }

    if (!hasSetTextureNames) {
      try {
        // Only set the camera texture names if the backgroundRenderer is properly initialized
        backgroundRenderer?.let {
          session.setCameraTextureNames(intArrayOf(it.cameraColorTexture.textureId))
          hasSetTextureNames = true
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to set camera texture names", e)
        showError("Failed to set camera texture names: $e")
        return
      }
    }

    displayRotationHelper.updateSessionIfNeeded(session)

    val frame =
      try {
        session.update()
      } catch (e: CameraNotAvailableException) {
        Log.e(TAG, "Camera not available during onDrawFrame", e)
        showError("Camera not available. Try restarting the app.")
        return
      }

    val camera = frame.camera

    trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

    if (camera.trackingState == TrackingState.TRACKING) {
      poseTracker.updatePose(camera.pose)
    }

    // Update the background renderer with the new frame
    try {
      backgroundRenderer?.updateDisplayGeometry(frame)
    } catch (e: Exception) {
      Log.e(TAG, "Exception updating display geometry", e)
      return
    }

    trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

    // Draw the background
    if (frame.timestamp != 0L) {
      try {
        backgroundRenderer?.drawBackground(render)
      } catch (e: Exception) {
        Log.e(TAG, "Exception drawing background", e)
        return
      }
    }

    if (camera.trackingState == TrackingState.PAUSED) {
      return
    }

    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)
    camera.getViewMatrix(viewMatrix, 0)
  }

  /** Checks if we detected at least one plane. */
  private fun Session.hasTrackingPlane() =
    getAllTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING }

  private fun showError(errorMessage: String) {
    activity.runOnUiThread {
      Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
    }
  }
}
