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
import kotlin.math.min
import kotlin.math.max
import kotlin.math.round
import java.util.LinkedList

/**
 * Helper class to track the phone's pose relative to an initial pose.
 * Provides functionality for manipulating position, rotation, and scale.
 */
class PoseTracker {
    private var initialPose: Pose? = null
    private var currentRelativePose: RelativePose = RelativePose()
    
    // Moving average filter parameters
    private val filterWindowSize = 5
    private val positionHistory = LinkedList<FloatArray>()
    private val rotationHistory = LinkedList<FloatArray>()
    
    // Manipulation parameters
    private var scale: Float = 1.0f
    private var userTranslation = floatArrayOf(0f, 0f, 0f)
    private var userRotation = floatArrayOf(0f, 0f, 0f)
    
    // Snapping parameters
    private var snapEnabled = false
    private var snapGridSize = 0.05f  // 5cm grid
    private var snapRotationDegrees = 15f

    /**
     * Reset the pose tracker to the initial state.
     * This clears all user translations, rotations, and resets the scale.
     */
    fun resetPose() {
        // Reset user manipulations
        userTranslation = floatArrayOf(0f, 0f, 0f)
        userRotation = floatArrayOf(0f, 0f, 0f)
        scale = 1.0f
        
        // Clear history for filters
        positionHistory.clear()
        rotationHistory.clear()
        
        // Reset the initial pose to null so it will be re-initialized on next frame
        initialPose = null
        
        // Reset the current relative pose
        currentRelativePose = RelativePose()
    }
    
    // Extension function to convert radians to degrees
    private fun Double.toDegrees() = this * 180.0 / PI
    
    // Extension function to convert degrees to radians
    private fun Float.toRadians() = this * PI.toFloat() / 180f

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
            
            // Apply user manipulations (translation, rotation, scale)
            val finalPosition = applyUserManipulations(avgPosition, avgRotation)
            val finalRotation = applyUserRotation(avgRotation)
            
            currentRelativePose = RelativePose(
                finalPosition[0],
                finalPosition[1],
                finalPosition[2],
                finalRotation[0],
                finalRotation[1],
                finalRotation[2],
                scale
            )
        }
    }
    
    /**
     * Applies user-defined manipulations to the position.
     */
    private fun applyUserManipulations(position: FloatArray, rotation: FloatArray): FloatArray {
        val result = FloatArray(3)
        
        // Apply scale
        result[0] = position[0] * scale
        result[1] = position[1] * scale
        result[2] = position[2] * scale
        
        // Apply user translation
        result[0] += userTranslation[0]
        result[1] += userTranslation[1]
        result[2] += userTranslation[2]
        
        // Apply snapping if enabled
        if (snapEnabled) {
            result[0] = snapToGrid(result[0])
            result[1] = snapToGrid(result[1])
            result[2] = snapToGrid(result[2])
        }
        
        return result
    }
    
    /**
     * Applies user-defined rotation adjustments.
     */
    private fun applyUserRotation(rotation: FloatArray): FloatArray {
        val result = FloatArray(3)
        
        // Apply user rotation
        result[0] = rotation[0] + userRotation[0]
        result[1] = rotation[1] + userRotation[1]
        result[2] = rotation[2] + userRotation[2]
        
        // Apply rotation snapping if enabled
        if (snapEnabled) {
            result[0] = snapToRotation(result[0])
            result[1] = snapToRotation(result[1])
            result[2] = snapToRotation(result[2])
        }
        
        return result
    }
    
    /**
     * Snaps a value to the nearest grid point.
     */
    private fun snapToGrid(value: Float): Float {
        return round(value / snapGridSize) * snapGridSize
    }
    
    /**
     * Snaps a rotation value to the nearest angle increment.
     */
    private fun snapToRotation(degrees: Float): Float {
        return round(degrees / snapRotationDegrees) * snapRotationDegrees
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
        
        // Reset user manipulations
        userTranslation = floatArrayOf(0f, 0f, 0f)
        userRotation = floatArrayOf(0f, 0f, 0f)
        scale = 1.0f
    }

    /**
     * Gets the current relative pose.
     */
    fun getCurrentRelativePose(): RelativePose {
        return currentRelativePose
    }
    
    /**
     * Translates the object by the specified amounts.
     */
    fun translate(dx: Float, dy: Float, dz: Float) {
        userTranslation[0] += dx
        userTranslation[1] += dy
        userTranslation[2] += dz
    }
    
    /**
     * Sets the absolute position of the object.
     */
    fun setPosition(x: Float, y: Float, z: Float) {
        userTranslation[0] = x
        userTranslation[1] = y
        userTranslation[2] = z
    }
    
    /**
     * Rotates the object by the specified angles in degrees.
     */
    fun rotate(dRoll: Float, dPitch: Float, dYaw: Float) {
        userRotation[0] += dRoll
        userRotation[1] += dPitch
        userRotation[2] += dYaw
    }
    
    /**
     * Sets the absolute rotation of the object in degrees.
     */
    fun setRotation(roll: Float, pitch: Float, yaw: Float) {
        userRotation[0] = roll
        userRotation[1] = pitch
        userRotation[2] = yaw
    }
    
    /**
     * Scales the object by the specified factor.
     */
    fun scale(factor: Float) {
        scale *= factor
        // Clamp scale to reasonable limits
        scale = max(0.1f, min(scale, 10.0f))
    }
    
    /**
     * Sets the absolute scale of the object.
     */
    fun setScale(newScale: Float) {
        scale = max(0.1f, min(newScale, 10.0f))
    }
    
    /**
     * Gets the current scale.
     */
    fun getScale(): Float {
        return scale
    }
    
    /**
     * Enables or disables position and rotation snapping.
     */
    fun setSnapEnabled(enabled: Boolean) {
        snapEnabled = enabled
    }
    
    /**
     * Sets the grid size for position snapping.
     */
    fun setSnapGridSize(gridSize: Float) {
        snapGridSize = max(0.01f, gridSize)
    }
    
    /**
     * Sets the angle increment for rotation snapping.
     */
    fun setSnapRotationDegrees(degrees: Float) {
        snapRotationDegrees = max(1f, degrees)
    }
    
    /**
     * Returns whether snapping is currently enabled.
     */
    fun isSnapEnabled(): Boolean {
        return snapEnabled
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
        val yaw: Float = 0f,
        val scale: Float = 1.0f
    ) {
        /**
         * Position as a float array [x, y, z]
         */
        val position: FloatArray
            get() = floatArrayOf(x, y, z)
        
        /**
         * Rotation as a float array [roll, pitch, yaw]
         */
        val rotation: FloatArray
            get() = floatArrayOf(roll, pitch, yaw)
        
        /**
         * Returns a formatted string representation of the pose.
         */
        fun toFormattedString(): String {
            return String.format(
                "Position: (%.2f, %.2f, %.2f)\nRotation: (%.2f, %.2f, %.2f)\nScale: %.2f",
                x, y, z, roll, pitch, yaw, scale
            )
        }
    }
}
