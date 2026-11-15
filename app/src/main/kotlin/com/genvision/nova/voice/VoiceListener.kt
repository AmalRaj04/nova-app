package com.genvision.nova.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** Why listening did not give a command. */
enum class VoiceProblem { DID_NOT_HEAR, NEEDS_PERMISSION, NOT_AVAILABLE, OTHER }

/**
 * One spoken command at a time, using Android's ON-DEVICE speech recogniser only, so NOVA stays
 * fully offline (no cloud recogniser is ever used). It needs Android 13 or newer with the offline
 * speech pack installed; otherwise [isAvailable] is false and the voice button is not shown.
 *
 * Call every method on the main thread. Results come back on the main thread.
 */
class VoiceListener(
    private val context: Context,
    private val onHeard: (List<String>) -> Unit,
    private val onProblem: (VoiceProblem) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    fun start() {
        cancel()
        if (!isAvailable(context)) return onProblem(VoiceProblem.NOT_AVAILABLE)
        val engine = createRecognizer() ?: return onProblem(VoiceProblem.NOT_AVAILABLE)
        recognizer = engine
        engine.setRecognitionListener(listener)
        beep() // tells the student that the phone is now listening
        engine.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            },
        )
    }

    /** Stop listening and free the recogniser. Safe to call at any time. */
    fun cancel() {
        recognizer?.let {
            it.setRecognitionListener(null)
            it.cancel()
            it.destroy()
        }
        recognizer = null
    }

    private fun createRecognizer(): SpeechRecognizer? =
        if (Build.VERSION.SDK_INT >= 33) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else null

    private fun beep() {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: RuntimeException) {
            // No beep is not a reason to fail.
        }
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            finish()
            if (heard.isEmpty()) onProblem(VoiceProblem.DID_NOT_HEAR) else onHeard(heard)
        }

        override fun onError(error: Int) {
            finish()
            onProblem(
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceProblem.DID_NOT_HEAR
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceProblem.NEEDS_PERMISSION
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> VoiceProblem.NOT_AVAILABLE
                    else -> VoiceProblem.OTHER
                },
            )
        }

        private fun finish() {
            recognizer?.destroy()
            recognizer = null
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        /** True only when the phone can recognise speech without the internet. */
        fun isAvailable(context: Context): Boolean =
            Build.VERSION.SDK_INT >= 33 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }
}
