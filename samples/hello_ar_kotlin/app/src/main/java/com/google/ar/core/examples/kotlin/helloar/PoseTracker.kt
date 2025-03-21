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

import com.google.ar.core.Pose
import kotlin.math.atan2
import kotlin.math.asin
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sign
import java.util.LinkedList

/**
 * Helper class to track the phone's pose relative to an initial pose.
 */
class PoseTracker {
    private var initialPose: Pose? = null
    private var currentRelativePose: RelativePose = RelativePose()
    
    // Moving average filter parameters
    private val filterWindowSize = 5
    private val positionHistory = LinkedList<FloatArray>()
    private val rotationHistory = LinkedList<FloatArray>()

    // Extension function to convert radians to degrees
    private fun Double.toDegrees() = this * 180.0 / PI

    /**
     * Updates the current pose and calculates the relative pose from the initial pose.
     * If this is the first pose received, it will be set as the initial pose.
     */
    fun updatePose(currentPose: Pose) {
        if (initialPose == null) {
            initialPose = currentPose
            currentRelativePose = RelativePose()
            
            // Initialize history with initial values
            val initialPosition = floatArrayOf(0f, 0f, 0f)
            val initialRotation = floatArrayOf(0f, 0f, 0f)
            
            for (i in 0 until filterWindowSize) {
                positionHistory.add(initialPosition.clone())
                rotationHistory.add(initialRotation.clone())
            }
        } else {
            // Calculate the relative pose from initial to current
            val relativePose = initialPose!!.inverse().compose(currentPose)
            
            // Extract position (x, y, z)
            val translation = relativePose.translation
            val position = floatArrayOf(translation[0], translation[1], translation[2])
            
            // Extract rotation (roll, pitch, yaw) in degrees
            val qx = relativePose.qx()
            val qy = relativePose.qy()
            val qz = relativePose.qz()
            val qw = relativePose.qw()
            
            // Convert quaternion to Euler angles (roll, pitch, yaw)
            // Roll (x-axis rotation)
            val sinr_cosp = 2 * (qw * qx + qy * qz)
            val cosr_cosp = 1 - 2 * (qx * qx + qy * qy)
            val roll = atan2(sinr_cosp.toDouble(), cosr_cosp.toDouble()).toDegrees()
            
            // Pitch (y-axis rotation)
            val sinp = 2 * (qw * qy - qz * qx)
            val pitch = if (abs(sinp) >= 1)
                (PI / 2 * sign(sinp.toDouble())).toDegrees() // Use 90 degrees if out of range
            else
                asin(sinp.toDouble()).toDegrees()
            
            // Yaw (z-axis rotation)
            val siny_cosp = 2 * (qw * qz + qx * qy)
            val cosy_cosp = 1 - 2 * (qy * qy + qz * qz)
            val yaw = atan2(siny_cosp.toDouble(), cosy_cosp.toDouble()).toDegrees()
            
            // Add current values to history
            positionHistory.add(position)
            rotationHistory.add(floatArrayOf(roll.toFloat(), pitch.toFloat(), yaw.toFloat()))
            
            // Remove oldest values if history is too large
            if (positionHistory.size > filterWindowSize) {
                positionHistory.removeFirst()
            }
            if (rotationHistory.size > filterWindowSize) {
                rotationHistory.removeFirst()
            }
            
            // Calculate average position and rotation
            val avgPosition = calculateAveragePosition()
            val avgRotation = calculateAverageRotation()
            
            currentRelativePose = RelativePose(
                avgPosition[0],
                avgPosition[1],
                avgPosition[2],
                avgRotation[0],
                avgRotation[1],
                avgRotation[2]
            )
        }
    }
    
    /**
     * Calculates the average position from the position history.
     */
    private fun calculateAveragePosition(): FloatArray {
        val avgPosition = floatArrayOf(0f, 0f, 0f)
        val size = positionHistory.size.toFloat()
        
        for (position in positionHistory) {
            avgPosition[0] += position[0]
            avgPosition[1] += position[1]
            avgPosition[2] += position[2]
        }
        
        avgPosition[0] /= size
        avgPosition[1] /= size
        avgPosition[2] /= size
        
        return avgPosition
    }
    
    /**
     * Calculates the average rotation from the rotation history.
     */
    private fun calculateAverageRotation(): FloatArray {
        val avgRotation = floatArrayOf(0f, 0f, 0f)
        val size = rotationHistory.size.toFloat()
        
        for (rotation in rotationHistory) {
            avgRotation[0] += rotation[0]
            avgRotation[1] += rotation[1]
            avgRotation[2] += rotation[2]
        }
        
        avgRotation[0] /= size
        avgRotation[1] /= size
        avgRotation[2] /= size
        
        return avgRotation
    }

    /**
     * Resets the initial pose to the current pose.
     */
    fun resetInitialPose(currentPose: Pose) {
        initialPose = currentPose
        currentRelativePose = RelativePose()
        
        // Reset history with initial values
        positionHistory.clear()
        rotationHistory.clear()
        
        val initialPosition = floatArrayOf(0f, 0f, 0f)
        val initialRotation = floatArrayOf(0f, 0f, 0f)
        
        for (i in 0 until filterWindowSize) {
            positionHistory.add(initialPosition.clone())
            rotationHistory.add(initialRotation.clone())
        }
    }

    /**
     * Gets the current relative pose.
     */
    fun getCurrentRelativePose(): RelativePose {
        return currentRelativePose
    }

    /**
     * Data class to hold the relative pose information.
     */
    data class RelativePose(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
        val roll: Float = 0f,
        val pitch: Float = 0f,
        val yaw: Float = 0f
    ) {
        /**
         * Returns a formatted string representation of the pose.
         */
        fun toFormattedString(): String {
            return String.format(
                "Position: (%.2f, %.2f, %.2f)\nRotation: (%.2f, %.2f, %.2f)",
                x, y, z, roll, pitch, yaw
            )
        }
    }
}
