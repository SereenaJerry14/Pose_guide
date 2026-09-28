package com.photopose.app.ui.viewfinder

import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.photopose.app.core.camera.CameraXManager
import com.photopose.app.core.mediapipe.MediaPipePoseHelper
import com.photopose.app.data.model.MatchState
import com.photopose.app.ui.theme.AccentAmber
import com.photopose.app.ui.theme.AccentCoral
import com.photopose.app.ui.theme.AccentNeonGreen
import com.photopose.app.ui.theme.DarkSurfaceTranslucent
import com.photopose.app.ui.theme.TextPrimary
import com.photopose.app.ui.viewmodel.PhotoPoseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewfinderScreen(viewModel: PhotoPoseViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val liveLandmarks by viewModel.liveLandmarks.collectAsState()
    val targetPose by viewModel.targetPose.collectAsState()
    val matchResult by viewModel.matchResult.collectAsState()
    val composition by viewModel.composition.collectAsState()

    val categories by viewModel.categories.collectAsState()
    val poses by viewModel.poses.collectAsState()
    val selectedCatId by viewModel.selectedCategoryId.collectAsState()

    var showGrid by remember { mutableStateOf(true) }
    var showGhost by remember { mutableStateOf(true) }
    var showAngleScanner by remember { mutableStateOf(true) }
    var showLensCleaningTip by remember { mutableStateOf(true) }
    var showPoseSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Camera & MediaPipe Reference holders
    var cameraManager by remember { mutableStateOf<CameraXManager?>(null) }
    val poseHelper = remember {
        MediaPipePoseHelper(
            context = context,
            onPoseDetected = { data -> viewModel.onPoseDataReceived(data) },
            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
        )
    }

    // Auto-Shutter listener
    LaunchedEffect(Unit) {
        viewModel.autoShutterEvent.collect {
            cameraManager?.takePhoto(
                onSuccess = { uri ->
                    Toast.makeText(context, "Auto-Captured Photo!", Toast.LENGTH_SHORT).show()
                    viewModel.onCaptureCompleted()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    viewModel.onCaptureCompleted()
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            poseHelper.close()
            cameraManager?.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. CameraX Preview Layer
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    val manager = CameraXManager(
                        context = ctx,
                        lifecycleOwner = lifecycleOwner,
                        previewView = this,
                        onFrameAvailable = { proxy ->
                            poseHelper.processImageProxy(proxy)
                        }
                    )
                    manager.startCamera()
                    cameraManager = manager
                }
            }
        )

        // 2. AR Viewfinder Overlay (Grid, Horizon Bar, Silhouette, Live Joints)
        ARViewfinderOverlay(
            modifier = Modifier.fillMaxSize(),
            liveLandmarks = liveLandmarks,
            targetPose = targetPose,
            matchResult = matchResult,
            composition = composition,
            showGrid = showGrid,
            showGhostGuide = showGhost
        )

        // 3. Top HUD (Lens Clean Tip, Match Score Badge, Controls, Angle Scanner Telemetry)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
        ) {
            // Lens Smudge & Clarity Pro Tip Banner (Dismissible)
            AnimatedVisibility(
                visible = showLensCleaningTip,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.90f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "✨", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pro Tip: Wipe camera lens with a soft cloth for crisp photos & no flare",
                                fontSize = 12.sp,
                                color = Color(0xFFF1F5F9),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { showLensCleaningTip = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss tip",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Match Score Pill
                val score = matchResult?.scorePercentage?.toInt() ?: 0
                val stateColor = when (matchResult?.state) {
                    MatchState.MATCHED -> AccentNeonGreen
                    MatchState.ADJUSTING -> AccentAmber
                    else -> AccentCoral
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceTranslucent)
                        .border(1.dp, stateColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Match $score%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Quick Toggle Buttons (Grid, Ghost Guide, Smart Angle Scanner)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceTranslucent)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = { showGrid = !showGrid }) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle Grid",
                            tint = if (showGrid) AccentNeonGreen else Color.White.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { showGhost = !showGhost }) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Toggle Ghost Silhouette",
                            tint = if (showGhost) AccentNeonGreen else Color.White.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { showAngleScanner = !showAngleScanner }) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Toggle Smart Angle Scanner",
                            tint = if (showAngleScanner) AccentNeonGreen else Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smart Angle Scanner Real-Time Telemetry Card
            val faceAngle = composition?.faceAngle
            if (showAngleScanner && faceAngle != null) {
                val angleColor = if (faceAngle.isFlatteringAngle) AccentNeonGreen else AccentAmber
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceTranslucent)
                        .border(1.dp, angleColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Angle Scanner: ${faceAngle.bestSideRecommendation}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = angleColor
                            )
                            Text(
                                text = "Turn: ${kotlin.math.abs(faceAngle.yawDegrees).toInt()}° • Tilt: ${faceAngle.pitchDegrees.toInt()}°",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = if (faceAngle.isFlatteringAngle) "FLATTERING 3/4" else "ADJUST ANGLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = angleColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Directorial Guidance Banner
            AnimatedVisibility(
                visible = matchResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val prompt = matchResult?.activeCoachingPrompt ?: ""
                if (prompt.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceTranslucent)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Coach: $prompt",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // 4. Bottom Controls (Pose Library Sheet Trigger, Shutter Button, Flip Camera)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pose Library Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showPoseSheet = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceTranslucent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Poses",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Poses", fontSize = 12.sp, color = TextPrimary)
                }

                // Shutter Button with Auto-Hold Circular Progress
                val holdProgress = matchResult?.holdProgress ?: 0f
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (holdProgress > 0f) {
                        CircularProgressIndicator(
                            progress = { holdProgress },
                            modifier = Modifier.size(84.dp),
                            color = AccentNeonGreen,
                            strokeWidth = 5.dp
                        )
                    }

                    // Main Shutter Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .clickable {
                                cameraManager?.takePhoto(
                                    onSuccess = { Toast.makeText(context, "Captured Photo!", Toast.LENGTH_SHORT).show() },
                                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(if (matchResult?.state == MatchState.MATCHED) AccentNeonGreen else Color.White)
                        )
                    }
                }

                // Flip Camera Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { cameraManager?.flipCamera() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceTranslucent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Flip", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }

        // 5. Pose Library Bottom Sheet Modal
        if (showPoseSheet) {
            PoseSelectorSheet(
                sheetState = sheetState,
                categories = categories,
                selectedCategoryId = selectedCatId,
                poses = poses,
                selectedPoseId = targetPose?.id ?: "",
                onCategorySelected = { catId -> viewModel.selectCategory(catId) },
                onPoseSelected = { selected -> viewModel.selectPose(selected) },
                onDismiss = { showPoseSheet = false }
            )
        }
    }
}
