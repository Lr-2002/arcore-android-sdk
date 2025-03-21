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

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * Helper to detect taps and double taps using Android GestureDetector, and pass the taps between UI thread and
 * render thread.
 */
class DoubleTapHelper(context: Context) : View.OnTouchListener {
    private val gestureDetector: GestureDetector
    private val queuedTaps: BlockingQueue<TapEvent> = ArrayBlockingQueue(16)

    init {
        gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    // Queue tap if there is space. Tap is lost if queue is full.
                    queuedTaps.offer(TapEvent(e, false))
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    // Queue double tap if there is space
                    queuedTaps.offer(TapEvent(e, true))
                    return true
                }

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }
            }
        )
    }

    /**
     * Polls for a tap.
     *
     * @return if a tap was queued, a TapEvent for the tap. Otherwise null if no taps are queued.
     */
    fun poll(): TapEvent? {
        return queuedTaps.poll()
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(motionEvent)
    }

    /**
     * Represents a tap event with information about whether it was a double tap.
     */
    data class TapEvent(
        val motionEvent: MotionEvent,
        val isDoubleTap: Boolean
    ) {
        val x: Float
            get() = motionEvent.x
        val y: Float
            get() = motionEvent.y
    }
}
