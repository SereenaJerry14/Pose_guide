package com.photopose.app.core.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.photopose.app.data.model.JointAngles
import com.photopose.app.data.model.SilhouettePoint
import com.photopose.app.data.model.TargetPoseTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optional Cloud VLM Reasoner:
 * Ingests a scene snapshot and generates a novel, aesthetically tailored pose
 */
class GeminiPoseSynthesizer(private val apiKey: String = "") {

    private val gson = Gson()

    companion object {
        private const val TAG = "GeminiPoseSynthesizer"
        private const val MODEL_NAME = "gemini-1.5-flash"
    }

    suspend fun synthesizePoseForScene(
        sceneSnapshot: Bitmap,
        sceneContext: String = "Outdoor / Street / Architecture"
    ): Result<TargetPoseTemplate> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            // Return default fallback when no API key configured
            return@withContext Result.success(getDefaultFallbackPose(sceneContext))
        }

        try {
            val generativeModel = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey
            )

            val prompt = """
                You are a master fashion and portrait photographer. Analyze this scene and generate the most flattering, natural pose tailored to the environment (e.g. leaning on a wall, sitting on stairs, walking, head tilt).
                
                Return ONLY valid JSON matching this schema:
                {
                  "title": "Creative Pose Name",
                  "description": "Short description of the pose and attitude",
                  "difficulty": "Easy" or "Medium" or "Advanced",
                  "framing": "Full Body" or "Medium Shot" or "Close-up",
                  "targetAngles": {
                    "leftElbow": 120.0,
                    "rightElbow": 140.0,
                    "leftShoulder": 30.0,
                    "rightShoulder": 20.0,
                    "leftKnee": 170.0,
                    "rightKnee": 150.0,
                    "torsoTilt": 4.0
                  },
                  "coachingPrompts": [
                    "Directorial prompt 1",
                    "Directorial prompt 2"
                  ],
                  "silhouetteKeypoints": [
                    {"id": 0, "x": 0.50, "y": 0.20},
                    {"id": 11, "x": 0.44, "y": 0.32},
                    {"id": 12, "x": 0.56, "y": 0.32},
                    {"id": 13, "x": 0.40, "y": 0.45},
                    {"id": 14, "x": 0.60, "y": 0.45},
                    {"id": 15, "x": 0.44, "y": 0.58},
                    {"id": 16, "x": 0.58, "y": 0.58},
                    {"id": 23, "x": 0.46, "y": 0.60},
                    {"id": 24, "x": 0.54, "y": 0.60},
                    {"id": 25, "x": 0.45, "y": 0.78},
                    {"id": 26, "x": 0.55, "y": 0.76}
                  ]
                }
            """.trimIndent()

            val inputContent = content {
                image(sceneSnapshot)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val jsonText = response.text?.let { extractJson(it) }
                ?: throw IllegalStateException("Empty response from AI")

            val dynamicPose = gson.fromJson(jsonText, TargetPoseTemplate::class.java)
            val completePose = dynamicPose.copy(
                id = "ai_dyn_${System.currentTimeMillis()}",
                categoryId = "ai_generated"
            )
            Result.success(completePose)
        } catch (e: Exception) {
            Log.e(TAG, "Dynamic pose synthesis failed: ${e.message}", e)
            Result.success(getDefaultFallbackPose(sceneContext))
        }
    }

    private fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            raw.substring(start, end + 1)
        } else {
            raw
        }
    }

    private fun getDefaultFallbackPose(context: String): TargetPoseTemplate {
        return TargetPoseTemplate(
            id = "fallback_context",
            categoryId = "context_aware",
            title = "Aesthetic Stance",
            description = "Natural balanced pose adapted to the frame.",
            difficulty = "Easy",
            framing = "Medium Shot",
            targetAngles = JointAngles(
                leftElbow = 135f,
                rightElbow = 145f,
                leftShoulder = 25f,
                rightShoulder = 20f,
                leftKnee = 175f,
                rightKnee = 165f,
                torsoTilt = 3f
            ),
            coachingPrompts = listOf(
                "Shift weight to your back leg",
                "Relax shoulders down",
                "Angle face 20 degrees toward the primary light"
            ),
            silhouetteKeypoints = listOf(
                SilhouettePoint(0, 0.50f, 0.18f),
                SilhouettePoint(11, 0.44f, 0.30f),
                SilhouettePoint(12, 0.56f, 0.30f),
                SilhouettePoint(13, 0.41f, 0.44f),
                SilhouettePoint(14, 0.59f, 0.44f),
                SilhouettePoint(15, 0.45f, 0.56f),
                SilhouettePoint(16, 0.55f, 0.56f),
                SilhouettePoint(23, 0.46f, 0.58f),
                SilhouettePoint(24, 0.54f, 0.58f),
                SilhouettePoint(25, 0.45f, 0.76f),
                SilhouettePoint(26, 0.55f, 0.74f)
            )
        )
    }
}
