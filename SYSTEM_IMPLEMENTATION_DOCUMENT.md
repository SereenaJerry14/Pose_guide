# Scene-Aware Photo Composition & Dynamic Pose Guidance System
## Technical Implementation Document & Architectural Blueprint

---

## 1. Executive Summary

This document describes the complete engineering implementation for **PhotoPose**, an intelligent real-time photo coach and composition assistant built natively for Android.

The application combines:
1. **Real-time On-Device Perception Loop** (30+ FPS) via **Android CameraX** and **Google MediaPipe Tasks Vision**, leveraging GPU hardware acceleration.
2. **Kinematic Angle Matching Engine** computing scale-invariant 3D angular deviations across 7 primary biomechanical limb vertices.
3. **Hardware Sensor Leveling** fusing device accelerometer/gyroscope signals to eliminate horizon tilts.
4. **Photographic Framing Rules** continuously scoring Rule of Thirds alignment and headroom.
5. **AR Viewfinder Overlay** in Jetpack Compose Canvas providing immediate visual alignment signals (Red/Yellow/Green) and ghost silhouettes.
6. **Throttled Voice Directing & Hands-Free Capture** activating native TTS directorial advice and auto-capturing high-res frames upon sustained pose stability.

---

## 2. End-to-End Pipeline Flowchart

```mermaid
flowchart TD
    %% Sensor & Input
    subgraph Hardware_Layer ["1. Hardware & Sensor Streams"]
        CAM["CameraX ImageAnalysis Stream\n(RGBA_8888 @ 30 FPS)"]
        IMU["SensorManager\n(TYPE_ROTATION_VECTOR / GRAVITY)"]
    end

    %% Real-time Processing
    subgraph Analysis_Thread ["2. Background Perception Pipeline"]
        MP["MediaPipe Pose Landmarker (GPU Delegate)\n- 33 3D Keypoints (x, y, z, visibility)"]
        KM["Kinematic Math Solver\n- Dot-product angle calculation\n- Angle difference penalties"]
        COMP["Composition Engine\n- Horizon roll evaluation (±2.5°)\n- Rule of Thirds offset & headroom"]
    end

    CAM -->|Bitmap / MPImage| MP
    MP -->|Landmarks (0..32)| KM
    MP -->|Landmarks (0..32)| COMP
    IMU -->|Pitch & Roll Degrees| COMP

    %% Repository / Catalog
    subgraph Catalog_Layer ["3. Pose Knowledge Base"]
        LIB["Curated Pose Library (Assets)\n- Target Angles & Coaching Prompts\n- Silhouette Keypoints"]
        VLM["Optional Gemini 1.5 Flash VLM\n- Scene-aware dynamic pose synthesis"]
    end

    LIB -->|Target Pose Template| KM
    VLM -.->|Dynamic Pose Template| KM

    %% ViewModel & State
    subgraph ViewModel_Layer ["4. Reactive ViewModel Orchestrator"]
        VM["PhotoPoseViewModel\n- Score computation (0-100%)\n- State transition (Red/Yellow/Green)\n- Hold timer (0.8s counter)"]
    end

    KM --> VM
    COMP --> VM

    %% User Interaction & AR
    subgraph UI_Layer ["5. Main UI & Viewfinder Layer"]
        AR["AR Canvas Viewfinder\n- Rule of Thirds Grid\n- Dynamic Horizon Reticle\n- Ghost Silhouette Guide\n- Live Color-Coded User Skeleton"]
        TTS["Directorial Audio Coach\n- Throttled TextToSpeech cues"]
        CAP["CameraX ImageCapture\n- Auto-capture on 0.8s hold"]
    end

    VM -->|Live Match Score & State| AR
    VM -->|Coaching Directives| TTS
    VM -->|Trigger on 100% Hold| CAP
```

---

## 3. Mathematical Foundations

### 3.1 Scale-Invariant Kinematic Joint Angle
To evaluate posture without dependence on subject height, distance from camera, or pixel scale, we calculate angles $\theta$ at key limb joints (e.g. elbow formed by shoulder $A$, elbow $B$, and wrist $C$):

$$\vec{u} = \vec{A} - \vec{B}, \quad \vec{v} = \vec{C} - \vec{B}$$

$$\cos(\theta) = \frac{\vec{u} \cdot \vec{v}}{\|\vec{u}\| \|\vec{v}\|} = \frac{u_x v_x + u_y v_y + u_z v_z}{\sqrt{u_x^2 + u_y^2 + u_z^2} \sqrt{v_x^2 + v_y^2 + v_z^2}}$$

$$\theta = \arccos\left(\text{clamp}(\cos(\theta), -1.0, 1.0)\right)$$

### 3.2 Continuous Pose Match Percentage
For $N$ evaluated joint angles ($N=7$), where each joint has a target angle $\theta_i^{\text{target}}$ and error is capped at $\Delta_{\max} = 45^\circ$:

$$\text{Error}_i = \min\left(|\theta_i^{\text{live}} - \theta_i^{\text{target}}|, \Delta_{\max}\right)$$

$$\text{TotalScore} = \left( 1 - \frac{\sum_{i=1}^N \text{Error}_i}{N \cdot \Delta_{\max}} \right) \times 100\%$$

- **State Classification**:
  - $\text{TotalScore} \ge 85\%$: **MATCHED (Green)**
  - $60\% \le \text{TotalScore} < 85\%$: **ADJUSTING (Yellow)**
  - $\text{TotalScore} < 60\%$: **OUT OF POSE (Red)**

### 3.3 Horizon Tilt Matrix
Using Android `SensorManager.getRotationMatrixFromVector(R, event.values)` and `SensorManager.getOrientation(R, values)`:

$$\text{Roll} = \text{degrees}(\text{values}[2])$$

If $|\text{Roll}| \le 2.5^\circ$, the camera is classified as level.

---

## 4. Hardware and Performance Optimization

- **Zero-Copy Frame Processing**: `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` prevents buffer queuing and guarantees that MediaPipe always analyzes the newest available frame.
- **Dedicated Background Executor**: MediaPipe inference runs on a single-thread background `cameraExecutor`, keeping the Android Main UI Thread completely free for 60 FPS Compose rendering.
- **Audio Throttling**: Directorial TTS prompts are filtered through a minimum interval window ($2.2\text{s}$ for new cues, $3.2\text{s}$ for repeats) to avoid audio clutter.
