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

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer

/**
 * A simple fragment that contains the AR view.
 */
class ArFragment : Fragment() {
    
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var renderer: HelloArRenderer
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Create a new GLSurfaceView for AR rendering
        surfaceView = GLSurfaceView(requireContext()).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        }
        
        return surfaceView
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get the renderer from the activity
        val activity = requireActivity() as HelloArActivity
        renderer = activity.renderer
        
        // Set up the renderer with the surface view
        SampleRender(surfaceView, renderer, requireActivity().assets)
    }
    
    override fun onResume() {
        super.onResume()
        surfaceView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        surfaceView.onPause()
    }
}
