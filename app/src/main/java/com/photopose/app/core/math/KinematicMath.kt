package com.photopose.app.core.math

import com.photopose.app.data.model.JointAngles
import com.photopose.app.data.model.LandmarkPoint
import com.photopose.app.data.model.MatchState
import com.photopose.app.data.model.PoseMatchResult
import com.photopose.app.data.model.TargetPoseTemplate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

object KinematicMath {

    /**
     * Calculates the angle in degrees between vectors BA and BC in 3D space.
     * Joint B is the vertex/hinge.
     */
    fun calculateAngle(
        a: LandmarkPoint,
        b: LandmarkPoint,
        c: LandmarkPoint
    ): Float {
        val v1x = a.x - b.x
        val v1y = a.y - b.y
        val v1z = a.z - b.z

        val v2x = c.x - b.x
        val v2y = c.y - b.y
        val v2z = c.z - b.z

        val dot = (v1x * v2x) + (v1y * v2y) + (v1z * v2z)
        val mag1 = sqrt((v1x * v1x + v1y * v1y + v1z * v1z).toDouble()).toFloat()
        val mag2 = sqrt((v2x * v2x + v2y * v2y + v2z * v2z).toDouble()).toFloat()

        if (mag1 * mag2 == 0f) return 0f
        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1.0f, 1.0f)
        return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
    }

    /**
     * Extracts key biomechanical joint angles from 33 MediaPipe landmarks:
     * 11: Left Shoulder, 12: Right Shoulder
     * 13: Left Elbow,    14: Right Elbow
     * 15: Left Wrist,    16: Right Wrist
     * 23: Left Hip,      24: Right Hip
     * 25: Left Knee,     26: Right Knee
     * 27: Left Ankle,    28: Right Ankle
     */
    fun extractJointAngles(landmarks: List<LandmarkPoint>): JointAngles {
        val map = landmarks.associateBy { it.id }

        fun getOrZero(id: Int) = map[id] ?: LandmarkPoint(id, 0f, 0f, 0f, 0f)

        val leftShoulder = getOrZero(11)
        val rightShoulder = getOrZero(12)
        val leftElbow = getOrZero(13)
        val rightElbow = getOrZero(14)
        val leftWrist = getOrZero(15)
        val rightWrist = getOrZero(16)
        val leftHip = getOrZero(23)
        val rightHip = getOrZero(24)
        val leftKnee = getOrZero(25)
        val rightKnee = getOrZero(26)
        val leftAnkle = getOrZero(27)
        val rightAnkle = getOrZero(28)

        val leftElbowAngle = calculateAngle(leftShoulder, leftElbow, leftWrist)
        val rightElbowAngle = calculateAngle(rightShoulder, rightElbow, rightWrist)
        val leftShoulderAngle = calculateAngle(leftElbow, leftShoulder, leftHip)
        val rightShoulderAngle = calculateAngle(rightElbow, rightShoulder, rightHip)
        val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)

        // Torso tilt relative to vertical
        val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2f
        val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2f
        val midHipX = (leftHip.x + rightHip.x) / 2f
        val midHipY = (leftHip.y + rightHip.y) / 2f
        val dx = midShoulderX - midHipX
        val dy = midHipY - midShoulderY
        val torsoTilt = if (dy != 0f) Math.toDegrees(kotlin.math.atan2(abs(dx.toDouble()), dy.toDouble())).toFloat() else 0f

        return JointAngles(
            leftElbow = leftElbowAngle,
            rightElbow = rightElbowAngle,
            leftShoulder = leftShoulderAngle,
            rightShoulder = rightShoulderAngle,
            leftKnee = leftKneeAngle,
            rightKnee = rightKneeAngle,
            torsoTilt = torsoTilt
        )
    }

    /**
     * Evaluates live kinematic angles against target pose template.
     * Computes normalized similarity score (0% - 100%) and pinpointed directorial coaching.
     */
    fun evaluatePoseMatch(
        live: JointAngles,
        target: TargetPoseTemplate,
        currentHoldProgress: Float = 0f
    ): PoseMatchResult {
        val t = target.targetAngles
        val mismatched = mutableListOf<String>()
        var totalError = 0f
        val maxAngleErrorPerJoint = 45.0f

        fun checkJoint(name: String, liveAngle: Float, targetAngle: Float, cue: String) {
            val diff = abs(liveAngle - targetAngle)
            totalError += diff.coerceAtMost(maxAngleErrorPerJoint)
            if (diff > 18.0f) {
                mismatched.add(cue)
            }
        }

        checkJoint("Left Elbow", live.leftElbow, t.leftElbow, "Adjust left arm angle")
        checkJoint("Right Elbow", live.rightElbow, t.rightElbow, "Adjust right arm angle")
        checkJoint("Left Shoulder", live.leftShoulder, t.leftShoulder, "Adjust left shoulder position")
        checkJoint("Right Shoulder", live.rightShoulder, t.rightShoulder, "Adjust right shoulder position")
        checkJoint("Left Knee", live.leftKnee, t.leftKnee, "Adjust left leg bend")
        checkJoint("Right Knee", live.rightKnee, t.rightKnee, "Adjust right leg bend")
        checkJoint("Torso Tilt", live.torsoTilt, t.torsoTilt, "Adjust body posture")

        val numJoints = 7
        val maxPossibleError = numJoints * maxAngleErrorPerJoint
        val rawScore = ((1f - (totalError / maxPossibleError)) * 100f).coerceIn(0f, 100f)

        val state = when {
            rawScore >= 85.0f -> MatchState.MATCHED
            rawScore >= 60.0f -> MatchState.ADJUSTING
            else -> MatchState.OUT_OF_POSE
        }

        val prompt = when (state) {
            MatchState.MATCHED -> "Perfect pose! Hold steady..."
            MatchState.ADJUSTING -> mismatched.firstOrNull() ?: target.coachingPrompts.firstOrNull() ?: "Fine-tuning pose..."
            MatchState.OUT_OF_POSE -> target.coachingPrompts.firstOrNull() ?: "Align your body with the silhouette guide"
        }

        return PoseMatchResult(
            scorePercentage = rawScore,
            state = state,
            activeCoachingPrompt = prompt,
            mismatchedJoints = mismatched,
            holdProgress = currentHoldProgress
        )
    }
}
