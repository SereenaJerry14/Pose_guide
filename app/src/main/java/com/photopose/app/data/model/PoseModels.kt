package com.photopose.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Normalized 3D landmark point [0.0 - 1.0] from MediaPipe Pose (33 points)
 */
data class LandmarkPoint(
    val id: Int,
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1.0f
)

/**
 * Angles for key kinematic joints (in degrees)
 */
data class JointAngles(
    @SerializedName("leftElbow") val leftElbow: Float = 0f,
    @SerializedName("rightElbow") val rightElbow: Float = 0f,
    @SerializedName("leftShoulder") val leftShoulder: Float = 0f,
    @SerializedName("rightShoulder") val rightShoulder: Float = 0f,
    @SerializedName("leftKnee") val leftKnee: Float = 0f,
    @SerializedName("rightKnee") val rightKnee: Float = 0f,
    @SerializedName("torsoTilt") val torsoTilt: Float = 0f
)

/**
 * Simplified 2D point for silhouette rendering
 */
data class SilhouettePoint(
    val id: Int,
    val x: Float,
    val y: Float
)

/**
 * Pose Category (Casual, Sitting, Wall Leaning, Portrait, Mirror)
 */
data class PoseCategory(
    val id: String,
    val title: String,
    val icon: String
)

/**
 * Complete target pose template loaded from catalog or dynamically generated
 */
data class TargetPoseTemplate(
    val id: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val framing: String,
    val targetAngles: JointAngles,
    val coachingPrompts: List<String>,
    val silhouetteKeypoints: List<SilhouettePoint>
)

/**
 * Encapsulates the entire JSON catalog
 */
data class PoseCatalog(
    val categories: List<PoseCategory>,
    val poses: List<TargetPoseTemplate>
)

/**
 * Real-time match status between live user and target pose
 */
enum class MatchState {
    OUT_OF_POSE,  // Red: Score < 60%
    ADJUSTING,    // Yellow: Score 60% - 84%
    MATCHED       // Green: Score >= 85%
}

/**
 * Real-time pose tracking result passed from MediaPipe to ViewModel
 */
data class PoseLandmarkData(
    val landmarks: List<LandmarkPoint>,
    val computedAngles: JointAngles,
    val timestampMs: Long
)

/**
 * Aggregated score with active coaching feedback
 */
data class PoseMatchResult(
    val scorePercentage: Float,
    val state: MatchState,
    val activeCoachingPrompt: String,
    val mismatchedJoints: List<String>,
    val holdProgress: Float = 0f // 0.0 to 1.0 for auto-capture hold timer
)
