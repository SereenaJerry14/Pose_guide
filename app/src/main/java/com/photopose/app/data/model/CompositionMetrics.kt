package com.photopose.app.data.model

/**
 * Evaluates subject placement relative to the Rule of Thirds grid lines
 */
data class RuleOfThirdsMetric(
    val errorDistance: Float, // Normalized distance [0.0 - 1.0] from nearest third line
    val isAligned: Boolean,
    val directive: String
)

/**
 * Evaluates headroom (distance between top of head and top of frame)
 */
data class HeadroomMetric(
    val headroomRatio: Float, // Top of head Y coordinate [0.0 - 1.0]
    val isOptimal: Boolean,   // Optimal is usually between 0.08 and 0.18
    val directive: String
)

/**
 * Evaluates device camera tilt/roll relative to true horizontal
 */
data class HorizonMetric(
    val rollDegrees: Float,   // Deviation from level (0 degrees)
    val isLevel: Boolean,     // True if abs(rollDegrees) <= 2.5 deg
    val directive: String
)

/**
 * Evaluates real-time 3D facial orientation (yaw, pitch, roll) and flattering angle detection
 */
data class FaceAngleMetric(
    val yawDegrees: Float,            // Left (-) / Right (+) head turn
    val pitchDegrees: Float,          // Chin Down (-) / Chin Up (+)
    val rollDegrees: Float,           // Head tilt sideways
    val isFlatteringAngle: Boolean,   // True when in flattering 15°-28° 3/4 turn
    val bestSideRecommendation: String,
    val directive: String
)

/**
 * Aggregated composition analysis for the camera viewfinder
 */
data class OverallComposition(
    val ruleOfThirds: RuleOfThirdsMetric,
    val headroom: HeadroomMetric,
    val horizon: HorizonMetric,
    val faceAngle: FaceAngleMetric? = null,
    val isReadyToShoot: Boolean,
    val primaryFramingDirective: String
)
