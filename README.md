# PhotoPose - AI Pose & Photo Coach (Android Native)

A high-performance Android application inspired by **[Photogenik: Pose & Photo Coach](https://photogenik.app/)**, designed to help subjects and photographers capture professional-grade, aesthetically composed photos.

---

## 🌟 Key Features

1. **100% On-Device Real-Time Pose Tracking**:
   - Uses **Google MediaPipe Tasks Vision (`PoseLandmarker`)** running on the phone's **GPU Delegate** at 30+ FPS.
   - Extracts 33 3D body keypoints without sending photos or biometric data to the cloud.

2. **Kinematic Angle Matching & Real-Time Scoring**:
   - Scale and translation-invariant 3D joint angle solver (elbows, shoulders, knees, torso tilt).
   - Dynamic match percentage (0% to 100%) with continuous visual feedback:
     - 🔴 **Red**: Out of pose ($< 60\%$)
     - 🟡 **Yellow**: Adjusting / Close ($60\% - 84\%$)
     - 🟢 **Green**: Perfect match ($\ge 85\%$)

3. **Sensor-Driven Photographic Composition**:
   - **Horizon Leveling**: Hardware accelerometer and gyroscope (`SensorManager`) provide real-time camera roll and pitch feedback with an animated horizon level reticle.
   - **Rule of Thirds**: Real-time evaluation of facial eye-lines against the upper third grid ($y \approx 0.33$).
   - **Headroom Guidance**: Continuously measures distance from frame top to prevent cramped framing.

4. **Curated Professional Pose Library**:
   - Pre-loaded with professional poses across categories:
     - 🚶 **Casual Standing**: Relaxed weight shift, hands in pockets.
     - ☕ **Sitting & Cafe**: Forearm rest, slight torso lean.
     - 🏛️ **Wall & Railing**: Shoulder rest, crossed ankles.
     - 👤 **Portrait & Close-up**: Flattering 3/4 angle, jawline definition.
     - 📱 **Mirror Selfie**: Classic chest-height phone angle.

5. **Directorial Voice Coaching**:
   - Native Android `TextToSpeech` delivers intelligent, throttled vocal guidance (*"Tilt camera up"*, *"Step back"*, *"Turn chin right"*).

6. **Hands-Free Auto-Capture**:
   - Holding a green pose match ($\ge 85\%$) steadily for **0.8 seconds** automatically triggers high-resolution photo capture with haptic feedback.

---

## 📁 Project Architecture

```
Photo_Pose/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── assets/
│   │   │   ├── pose_landmarker_full.task   # Google MediaPipe 9.4MB binary
│   │   │   └── pose_library.json           # Catalog of poses & target angles
│   │   └── java/com/photopose/app/
│   │       ├── MainActivity.kt             # App entry & camera permissions
│   │       ├── data/
│   │       │   ├── model/                  # Data classes (Pose, Composition, MatchResult)
│   │       │   └── repository/             # Pose catalog loader
│   │       ├── core/
│   │       │   ├── camera/                 # CameraX preview, analysis & photo capture
│   │       │   ├── mediapipe/              # MediaPipe Pose Landmarker GPU wrapper
│   │       │   ├── math/                   # Kinematic angle math & composition rules
│   │       │   ├── sensor/                 # IMU sensor leveling (Pitch/Roll)
│   │       │   ├── tts/                    # Directorial voice coach
│   │       │   └── ai/                     # Gemini 1.5 Flash Vision cloud extension
│   │       └── ui/
│   │           ├── theme/                  # Photogenik-inspired dark & neon theme
│   │           ├── viewfinder/             # ARViewfinderOverlay, PoseSelectorSheet, ViewfinderScreen
│   │           └── viewmodel/              # PhotoPoseViewModel
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml                  # Version catalog
│   └── wrapper/
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 🚀 How to Run in Android Studio

1. Open **Android Studio** (Koala / Ladybug or newer recommended).
2. Select **Open** and browse to this directory: `K:\projects\Photo_Pose`.
3. Allow Gradle to perform its initial sync.
4. Connect an Android phone via USB (with **USB Debugging** enabled in Developer Options) or launch an Android Virtual Device (AVD) emulator configured with a camera feed.
5. Click **Run (`Shift + F10`)**.
6. Grant camera permission when prompted, select a pose from the **Poses** button, and line up your body with the ghost silhouette!
