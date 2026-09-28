package com.photopose.app.core.math

import com.photopose.app.data.model.HeadroomMetric
import com.photopose.app.data.model.HorizonMetric
import com.photopose.app.data.model.LandmarkPoint
import com.photopose.app.data.model.OverallComposition
import com.photopose.app.data.model.RuleOfThirdsMetric
import kotlin.math.abs

object CompositionMath {

    /**
     * Evaluates visual composition metrics based on landmarks and device sensor tilt
     */
    fun evaluateComposition(
        landmarks: List<LandmarkPoint>,
        deviceRollDegrees: Float
    ): OverallComposition {
        val nose = landmarks.find { it.id == 0 }

        // 1. Rule of Thirds Evaluation
        val ruleOfThirds = if (nose != null) {
            val upperThirdY = 0.333f
            val dist = abs(nose.y - upperThirdY)
            val isAligned = dist < 0.08f
            val directive = when {
                isAligned -> "Framing aligned with Rule of Thirds"
                nose.y < upperThirdY -> "Tilt camera slightly up or step forward"
                else -> "Tilt camera slightly down or step back"
            }
            RuleOfThirdsMetric(dist, isAligned, directive)
        } else {
            RuleOfThirdsMetric(0.5f, false, "Position subject within frame")
        }

        // 2. Headroom Evaluation
        val headroom = if (nose != null) {
            val topOfHeadY = (nose.y - 0.10f).coerceAtLeast(0f)
            val isOptimal = topOfHeadY in 0.06f..0.20f
            val directive = when {
                isOptimal -> "Headroom is optimal"
                topOfHeadY < 0.06f -> "Too close to top — tilt up slightly"
                else -> "Excess headroom — tilt down slightly"
            }
            HeadroomMetric(topOfHeadY, isOptimal, directive)
        } else {
            HeadroomMetric(0.5f, false, "Keep head inside the viewfinder")
        }

        // 3. Horizon Leveling Evaluation
        val isLevel = abs(deviceRollDegrees) <= 2.5f
        val horizonDirective = when {
            isLevel -> "Camera is level"
            deviceRollDegrees > 2.5f -> "Rotate camera counter-clockwise"
            else -> "Rotate camera clockwise"
        }
        val horizon = HorizonMetric(deviceRollDegrees, isLevel, horizonDirective)

        // 4. Smart Angle Scanner (3D Facial Orientation & Flattering Angle Analysis)
        val faceAngle = calculateFaceAngle(landmarks)

        // Synthesize primary priority directive
        val primaryDirective = when {
            !horizon.isLevel -> horizon.directive
            !headroom.isOptimal -> headroom.directive
            !ruleOfThirds.isAligned -> ruleOfThirds.directive
            faceAngle != null && !faceAngle.isFlatteringAngle -> faceAngle.directive
            else -> "Framing and level look great!"
        }

        val isReady = horizon.isLevel && headroom.isOptimal && ruleOfThirds.isAligned

        return OverallComposition(
            ruleOfThirds = ruleOfThirds,
            headroom = headroom,
            horizon = horizon,
            faceAngle = faceAngle,
            isReadyToShoot = isReady,
            primaryFramingDirective = primaryDirective
        )
    }

    private fun calculateFaceAngle(landmarks: List<LandmarkPoint>): com.photopose.app.data.model.FaceAngleMetric? {
        val nose = landmarks.find { it.id == 0 } ?: return null
        val leftEye = landmarks.find { it.id == 2 } ?: landmarks.find { it.id == 1 } ?: return null
        val rightEye = landmarks.find { it.id == 5 } ?: landmarks.find { it.id == 4 } ?: return null
        val leftEar = landmarks.find { it.id == 7 }
        val rightEar = landmarks.find { it.id == 8 }
        val leftMouth = landmarks.find { it.id == 9 }
        val rightMouth = landmarks.find { it.id == 10 }

        // Yaw calculation: nose position relative to ears or eyes
        val yawDeg = if (leftEar != null && rightEar != null) {
            val earMidX = (leftEar.x + rightEar.x) / 2f
            val earSpan = abs(leftEar.x - rightEar.x).coerceAtLeast(0.01f)
            val norm = (nose.x - earMidX) / (earSpan / 2f)
            (norm * 45f).coerceIn(-60f, 60f)
        } else {
            val distLeft = abs(nose.x - leftEye.x)
            val distRight = abs(nose.x - rightEye.x)
            val ratio = (distRight - distLeft) / (distLeft + distRight).coerceAtLeast(0.01f)
            (ratio * 50f).coerceIn(-60f, 60f)
        }

        // Pitch calculation: vertical relationship between eye line, nose, and mouth
        val eyeMidY = (leftEye.y + rightEye.y) / 2f
        val mouthMidY = if (leftMouth != null && rightMouth != null) {
            (leftMouth.y + rightMouth.y) / 2f
        } else {
            nose.y + 0.08f
        }
        val faceHeight = (mouthMidY - eyeMidY).coerceAtLeast(0.01f)
        val noseRelY = (nose.y - eyeMidY) / faceHeight
        val pitchDeg = ((noseRelY - 0.58f) * -90f).coerceIn(-45f, 45f)

        // Roll calculation: eye line tilt
        val dX = (rightEye.x - leftEye.x).toDouble()
        val dY = (rightEye.y - leftEye.y).toDouble()
        val rollDeg = Math.toDegrees(kotlin.math.atan2(dY, dX)).toFloat()

        // Flattering Angle evaluation (14° to 28° turn creates natural light-falloff & sculpted jawline)
        val absYaw = abs(yawDeg)
        val isFlattering = (absYaw in 14f..28f) && (pitchDeg in -12f..3f)

        val bestSide = when {
            yawDeg < -8f -> "Left 3/4 Profile"
            yawDeg > 8f -> "Right 3/4 Profile"
            else -> "Center Frontal"
        }

        val directive = when {
            isFlattering -> "✨ Flattering 3/4 angle locked!"
            absYaw < 14f -> "Turn head slightly 15° for a more defined jawline"
            absYaw > 28f -> "Turn head slightly back toward the camera"
            pitchDeg < -12f -> "Raise chin slightly"
            pitchDeg > 3f -> "Lower chin slightly for better eye contact"
            else -> "Angle looks natural"
        }

        return com.photopose.app.data.model.FaceAngleMetric(
            yawDegrees = yawDeg,
            pitchDegrees = pitchDeg,
            rollDegrees = rollDeg,
            isFlatteringAngle = isFlattering,
            bestSideRecommendation = bestSide,
            directive = directive
        )
    }
}
