package com.photopose.app.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Delivers directional coaching voice prompts with smart interval throttling
 */
class DirectorialTTSHelper(
    context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var lastSpokenCue: String = ""
    private var lastSpokenTimestampMs: Long = 0L

    companion object {
        private const val TAG = "DirectorialTTS"
        private const val MIN_REPEAT_INTERVAL_MS = 3200L // 3.2 seconds minimum between identical cues
        private const val MIN_NEW_CUE_INTERVAL_MS = 2200L // 2.2 seconds minimum between different cues
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isInitialized = !(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            tts?.setSpeechRate(1.05f) // Natural energetic directorial tone
            tts?.setPitch(1.0f)
            Log.i(TAG, "TTS initialized successfully")
            onInitComplete(isInitialized)
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
            onInitComplete(false)
        }
    }

    fun speakCoachingCue(cue: String, isHighPriority: Boolean = false) {
        if (!isInitialized || cue.isBlank()) return

        val now = System.currentTimeMillis()
        val isIdentical = cue == lastSpokenCue
        val interval = if (isIdentical) MIN_REPEAT_INTERVAL_MS else MIN_NEW_CUE_INTERVAL_MS

        if (!isHighPriority && (now - lastSpokenTimestampMs < interval)) {
            // Throttled to avoid overwhelming the subject
            return
        }

        lastSpokenCue = cue
        lastSpokenTimestampMs = now

        val queueMode = if (isHighPriority) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(cue, queueMode, null, cue.hashCode().toString())
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
