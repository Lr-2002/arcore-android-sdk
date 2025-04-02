# AR Teleoperation for Android

[![ARCore](https://img.shields.io/badge/ARCore-Powered-brightgreen)](https://developers.google.com/ar)

A mobile application for augmented reality teleoperation of robotic systems, built on Google's ARCore platform.

## Overview

This project extends the [Google ARCore Android SDK](https://github.com/google-ar/arcore-android-sdk) to enable AR-based teleoperation. It's designed to work with [MujocoAR](https://github.com/omarrayyann/MujocoAR) for robotic simulation and control.

## Getting Started

### Prerequisites

- Android device with [ARCore support](https://developers.google.com/ar/devices)
- Google Play Services for AR (ARCore) installed
- Camera permissions enabled on your device

### Installation

#### Option 1: Direct APK Install
1. Download the APK from [here](https://github.com/Lr-2002/arcore-android-sdk/blob/main/samples/hello_ar_kotlin/teleop/app-debug.apk)
2. Install the APK on your Android device
3. Grant camera permissions when prompted

#### Option 2: Build from Source
Clone this repository and build using Android Studio.

### Usage

1. Set up the server side:
   ```bash
   cd samples/hello_ar_kotlin
   mjpython server/simple.py  # This will display a box in the Mujoco environment
   ```

2. On your Android device:
   - Launch the AR Teleoperation app
   - Connect to the server by entering the IP address and port
   - Follow the on-screen instructions to establish the connection

For detailed usage instructions, refer to the [MujocoAR documentation](https://github.com/omarrayyann/MujocoAR).

## Acknowledgements

- Thanks to [Omar Rayyan](https://github.com/omarrayyann) for the MujocoAR project and collaboration
- Based on [Google's ARCore Android SDK](https://github.com/google-ar/arcore-android-sdk)

## Future Updates

The application will be available on F-Droid and Google Play Store in future releases.
