package com.photopose.app.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photopose.app.core.math.CompositionMath
import com.photopose.app.core.math.KinematicMath
import com.photopose.app.core.sensor.DeviceOrientationSensor
import com.photopose.app.core.tts.DirectorialTTSHelper
import com.photopose.app.data.model.LandmarkPoint
import com.photopose.app.data.model.MatchState
import com.photopose.app.data.model.OverallComposition
import com.photopose.app.data.model.PoseCategory
import com.photopose.app.data.model.PoseLandmarkData
import com.photopose.app.data.model.PoseMatchResult
import com.photopose.app.data.model.TargetPoseTemplate
import com.photopose.app.data.repository.PoseRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoPoseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PoseRepository(application)
    private val ttsHelper = DirectorialTTSHelper(application)
    private val orientationSensor = DeviceOrientationSensor(application) { roll, _ ->
        _currentRollDegrees.value = roll
        recalculateComposition()
    }

    // UI State Holders
    private val _categories = MutableStateFlow<List<PoseCategory>>(emptyList())
    val categories: StateFlow<List<PoseCategory>> = _categories.asStateFlow()

    private val _poses = MutableStateFlow<List<TargetPoseTemplate>>(emptyList())
    val poses: StateFlow<List<TargetPoseTemplate>> = _poses.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String>("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _targetPose = MutableStateFlow<TargetPoseTemplate?>(null)
    val targetPose: StateFlow<TargetPoseTemplate?> = _targetPose.asStateFlow()

    private val _liveLandmarks = MutableStateFlow<List<LandmarkPoint>>(emptyList())
    val liveLandmarks: StateFlow<List<LandmarkPoint>> = _liveLandmarks.asStateFlow()

    private val _matchResult = MutableStateFlow<PoseMatchResult?>(null)
    val matchResult: StateFlow<PoseMatchResult?> = _matchResult.asStateFlow()

    private val _composition = MutableStateFlow<OverallComposition?>(null)
    val composition: StateFlow<OverallComposition?> = _composition.asStateFlow()

    private val _currentRollDegrees = MutableStateFlow(0f)

    // Auto-Shutter Trigger Flow
    private val _autoShutterEvent = MutableSharedFlow<Unit>()
    val autoShutterEvent: SharedFlow<Unit> = _autoShutterEvent.asSharedFlow()

    // Hold Timer State
    private var greenStartTimeMs: Long = 0L
    private val holdTargetDurationMs = 800L // 0.8s hold time required for auto-capture
    private var isAutoShutterLocked = false

    init {
        loadCatalog()
        orientationSensor.start()
    }

    private fun loadCatalog() {
        val cats = repository.getCategories()
        _categories.value = cats
        val allPoses = repository.getPosesByCategory("all")
        _poses.value = allPoses
        if (allPoses.isNotEmpty()) {
            _targetPose.value = allPoses[0] // Select first pose by default
        }
    }

    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
        _poses.value = repository.getPosesByCategory(categoryId)
    }

    fun selectPose(pose: TargetPoseTemplate) {
        _targetPose.value = pose
        greenStartTimeMs = 0L
        isAutoShutterLocked = false
        ttsHelper.speakCoachingCue(pose.coachingPrompts.firstOrNull() ?: pose.title, isHighPriority = true)
    }

    fun onPoseDataReceived(data: PoseLandmarkData) {
        _liveLandmarks.value = data.landmarks
        val activeTarget = _targetPose.value ?: return

        recalculateComposition()

        if (data.landmarks.isEmpty()) {
            greenStartTimeMs = 0L
            _matchResult.value = PoseMatchResult(
                scorePercentage = 0f,
                state = MatchState.OUT_OF_POSE,
                activeCoachingPrompt = "Step into frame to begin",
                mismatchedJoints = emptyList()
            )
            return
        }

        // 1. Calculate pose match
        var holdProgress = 0f
        val tempResult = KinematicMath.evaluatePoseMatch(data.computedAngles, activeTarget, 0f)

        // 2. Auto-Shutter Hold Progress
        if (tempResult.state == MatchState.MATCHED) {
            val now = SystemClock.uptimeMillis()
            if (greenStartTimeMs == 0L) {
                greenStartTimeMs = now
            }
            val elapsed = now - greenStartTimeMs
            holdProgress = (elapsed.toFloat() / holdTargetDurationMs).coerceIn(0f, 1f)

            if (holdProgress >= 1f && !isAutoShutterLocked) {
                isAutoShutterLocked = true
                ttsHelper.speakCoachingCue("Holding steady, photo captured!", isHighPriority = true)
                viewModelScope.launch {
                    _autoShutterEvent.emit(Unit)
                }
            }
        } else {
            greenStartTimeMs = 0L
            isAutoShutterLocked = false
        }

        val finalResult = tempResult.copy(holdProgress = holdProgress)
        _matchResult.value = finalResult

        // Deliver coaching audio directives
        if (finalResult.activeCoachingPrompt.isNotBlank()) {
            ttsHelper.speakCoachingCue(finalResult.activeCoachingPrompt)
        }
    }

    private fun recalculateComposition() {
        val landmarks = _liveLandmarks.value
        val roll = _currentRollDegrees.value
        _composition.value = CompositionMath.evaluateComposition(landmarks, roll)
    }

    fun onCaptureCompleted() {
        // Reset hold state after capture
        greenStartTimeMs = 0L
        isAutoShutterLocked = false
    }

    override fun onCleared() {
        super.onCleared()
        orientationSensor.stop()
        ttsHelper.shutdown()
    }
}
