package com.photopose.app.ui.viewfinder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import com.photopose.app.data.model.LandmarkPoint
import com.photopose.app.data.model.MatchState
import com.photopose.app.data.model.OverallComposition
import com.photopose.app.data.model.PoseMatchResult
import com.photopose.app.data.model.SilhouettePoint
import com.photopose.app.data.model.TargetPoseTemplate
import com.photopose.app.ui.theme.AccentAmber
import com.photopose.app.ui.theme.AccentCoral
import com.photopose.app.ui.theme.AccentNeonGreen
import com.photopose.app.ui.theme.GridLineColor
import com.photopose.app.ui.theme.HorizonLevelColor
import com.photopose.app.ui.theme.HorizonUnlevelColor

@Composable
fun ARViewfinderOverlay(
    modifier: Modifier = Modifier,
    liveLandmarks: List<LandmarkPoint>,
    targetPose: TargetPoseTemplate?,
    matchResult: PoseMatchResult?,
    composition: OverallComposition?,
    showGrid: Boolean = true,
    showGhostGuide: Boolean = true
) {
    val stateColor = when (matchResult?.state) {
        MatchState.MATCHED -> AccentNeonGreen
        MatchState.ADJUSTING -> AccentAmber
        else -> AccentCoral
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw Rule of Thirds Grid
        if (showGrid) {
            val oneThirdW = width / 3f
            val twoThirdW = (width * 2f) / 3f
            val oneThirdH = height / 3f
            val twoThirdH = (height * 2f) / 3f

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

            // Vertical third lines
            drawLine(GridLineColor, Offset(oneThirdW, 0f), Offset(oneThirdW, height), strokeWidth = 1.5f, pathEffect = dashEffect)
            drawLine(GridLineColor, Offset(twoThirdW, 0f), Offset(twoThirdW, height), strokeWidth = 1.5f, pathEffect = dashEffect)

            // Horizontal third lines
            drawLine(GridLineColor, Offset(0f, oneThirdH), Offset(width, oneThirdH), strokeWidth = 1.5f, pathEffect = dashEffect)
            drawLine(GridLineColor, Offset(0f, twoThirdH), Offset(width, twoThirdH), strokeWidth = 1.5f, pathEffect = dashEffect)
        }

        // 2. Draw Horizon Level Indicator
        composition?.let { comp ->
            val roll = comp.horizon.rollDegrees
            val isLevel = comp.horizon.isLevel
            val levelColor = if (isLevel) HorizonLevelColor else HorizonUnlevelColor

            val centerY = height * 0.45f
            val barHalfLength = width * 0.18f

            // Rotate canvas for horizon tilt
            rotate(degrees = -roll, pivot = Offset(width / 2f, centerY)) {
                // Left marker
                drawLine(
                    color = levelColor,
                    start = Offset((width / 2f) - barHalfLength, centerY),
                    end = Offset((width / 2f) - (barHalfLength * 0.35f), centerY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
                // Center reticle
                drawCircle(
                    color = levelColor,
                    radius = 4f,
                    center = Offset(width / 2f, centerY)
                )
                // Right marker
                drawLine(
                    color = levelColor,
                    start = Offset((width / 2f) + (barHalfLength * 0.35f), centerY),
                    end = Offset((width / 2f) + barHalfLength, centerY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Draw Target Ghost Silhouette Guide
        if (showGhostGuide && targetPose != null) {
            val ghostPoints = targetPose.silhouetteKeypoints
            val ghostColor = Color.White.copy(alpha = 0.45f)
            val ghostBoneColor = Color.White.copy(alpha = 0.30f)

            // Draw ghost connections (bones)
            drawSilhouetteSkeleton(
                points = ghostPoints,
                width = width,
                height = height,
                boneColor = ghostBoneColor,
                jointColor = ghostColor
            )
        }

        // 4. Draw Live Subject Skeleton & Face Angle Indicator
        if (liveLandmarks.isNotEmpty()) {
            drawLiveUserSkeleton(
                landmarks = liveLandmarks,
                faceAngle = composition?.faceAngle,
                width = width,
                height = height,
                lineColor = stateColor.copy(alpha = 0.85f),
                jointColor = stateColor
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSilhouetteSkeleton(
    points: List<SilhouettePoint>,
    width: Float,
    height: Float,
    boneColor: Color,
    jointColor: Color
) {
    val pointMap = points.associateBy { it.id }

    fun connect(id1: Int, id2: Int) {
        val p1 = pointMap[id1] ?: return
        val p2 = pointMap[id2] ?: return
        drawLine(
            color = boneColor,
            start = Offset(p1.x * width, p1.y * height),
            end = Offset(p2.x * width, p2.y * height),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
    }

    // Connect silhouette bones
    connect(11, 12) // Shoulders
    connect(11, 13); connect(13, 15) // Left Arm
    connect(12, 14); connect(14, 16) // Right Arm
    connect(11, 23); connect(12, 24) // Torso sides
    connect(23, 24) // Hips
    connect(23, 25); connect(25, 27) // Left Leg
    connect(24, 26); connect(26, 28) // Right Leg

    // Head circle
    pointMap[0]?.let { head ->
        drawCircle(
            color = jointColor,
            radius = 16f,
            center = Offset(head.x * width, head.y * height)
        )
    }

    // Joint dots
    for (pt in points) {
        if (pt.id != 0) {
            drawCircle(
                color = jointColor,
                radius = 6f,
                center = Offset(pt.x * width, pt.y * height)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiveUserSkeleton(
    landmarks: List<LandmarkPoint>,
    faceAngle: com.photopose.app.data.model.FaceAngleMetric?,
    width: Float,
    height: Float,
    lineColor: Color,
    jointColor: Color
) {
    val map = landmarks.associateBy { it.id }

    fun connect(id1: Int, id2: Int) {
        val p1 = map[id1] ?: return
        val p2 = map[id2] ?: return
        if (p1.visibility > 0.4f && p2.visibility > 0.4f) {
            drawLine(
                color = lineColor,
                start = Offset(p1.x * width, p1.y * height),
                end = Offset(p2.x * width, p2.y * height),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }

    // Draw main kinematic limbs
    connect(11, 12) // Shoulders
    connect(11, 13); connect(13, 15) // Left arm
    connect(12, 14); connect(14, 16) // Right arm
    connect(11, 23); connect(12, 24) // Torso
    connect(23, 24) // Hips
    connect(23, 25); connect(25, 27) // Left leg
    connect(24, 26); connect(26, 28) // Right leg

    // Joint landmarks
    for (lm in landmarks) {
        if (lm.visibility > 0.45f && lm.id in listOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)) {
            val r = if (lm.id == 0) 12f else 7f
            drawCircle(
                color = jointColor,
                radius = r,
                center = Offset(lm.x * width, lm.y * height)
            )
        }
    }

    // Golden / Neon flattering face angle halo ring
    if (faceAngle?.isFlatteringAngle == true) {
        map[0]?.let { nose ->
            drawCircle(
                color = Color(0xFF00FF88).copy(alpha = 0.25f),
                radius = 32f,
                center = Offset(nose.x * width, nose.y * height)
            )
            drawCircle(
                color = Color(0xFF00FF88).copy(alpha = 0.60f),
                radius = 24f,
                center = Offset(nose.x * width, nose.y * height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
        }
    }
}
