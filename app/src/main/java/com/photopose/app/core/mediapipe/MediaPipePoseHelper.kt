package com.photopose.app.core.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.photopose.app.core.math.KinematicMath
import com.photopose.app.data.model.LandmarkPoint
import com.photopose.app.data.model.PoseLandmarkData

/**
 * Wraps Google MediaPipe PoseLandmarker for real-time 33-keypoint tracking on Android
 */
class MediaPipePoseHelper(
    private val context: Context,
    private val onPoseDetected: (PoseLandmarkData) -> Unit,
    private val onError: (String) -> Unit
) {

    private var poseLandmarker: PoseLandmarker? = null

    companion object {
        private const val TAG = "MediaPipePoseHelper"
        const val MODEL_NAME = "pose_landmarker_full.task"
    }

    init {
        setupPoseLandmarker()
    }

    fun setupPoseLandmarker() {
        try {
            // Try GPU Delegate first for maximum performance, fallback to CPU if unavailable
            val baseOptions = try {
                BaseOptions.builder()
                    .setDelegate(Delegate.GPU)
                    .setModelAssetPath(MODEL_NAME)
                    .build()
            } catch (e: Exception) {
                Log.w(TAG, "GPU delegate unavailable, falling back to CPU", e)
                BaseOptions.builder()
                    .setDelegate(Delegate.CPU)
                    .setModelAssetPath(MODEL_NAME)
                    .build()
            }

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result: PoseLandmarkerResult, _: MPImage ->
                    processResult(result)
                }
                .setErrorListener { error: RuntimeException ->
                    Log.e(TAG, "MediaPipe error: ${error.message}", error)
                    onError(error.message ?: "Unknown MediaPipe error")
                }
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.i(TAG, "MediaPipe Pose Landmarker successfully initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Pose Landmarker: ${e.message}", e)
            onError("Pose tracking model load failed: ${e.message}")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(imageProxy: ImageProxy) {
        val landmarker = poseLandmarker ?: run {
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            // Rotate bitmap if necessary so landmarks align correctly with portrait screen
            val finalBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val mpImage = BitmapImageBuilder(finalBitmap).build()
            val timestampMs = imageProxy.imageInfo.timestamp / 1_000_000L

            landmarker.detectAsync(mpImage, timestampMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image proxy: ${e.message}", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun processResult(result: PoseLandmarkerResult) {
        val landmarksList = result.landmarks()
        if (landmarksList.isNullOrEmpty() || landmarksList[0].isEmpty()) {
            // No subject detected in frame
            onPoseDetected(
                PoseLandmarkData(
                    landmarks = emptyList(),
                    computedAngles = KinematicMath.extractJointAngles(emptyList()),
                    timestampMs = System.currentTimeMillis()
                )
            )
            return
        }

        val firstPersonLandmarks = landmarksList[0]
        val points = firstPersonLandmarks.mapIndexed { index, mark ->
            LandmarkPoint(
                id = index,
                x = mark.x(),
                y = mark.y(),
                z = mark.z(),
                visibility = mark.visibility().orElse(1.0f)
            )
        }

        val angles = KinematicMath.extractJointAngles(points)

        onPoseDetected(
            PoseLandmarkData(
                landmarks = points,
                computedAngles = angles,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    fun close() {
        try {
            poseLandmarker?.close()
            poseLandmarker = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Pose Landmarker", e)
        }
    }
}
