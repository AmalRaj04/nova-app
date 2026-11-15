package com.genvision.nova.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Android TextToSpeech in English. TextToSpeech starts asynchronously, so anything spoken
 * before it is ready is queued and played once it reports ready (FR-16).
 * [onError] is called on the main thread if speech or the English voice is unavailable.
 */
class AndroidSpeaker(context: Context, private val onError: (String) -> Unit) : Speaker {

    private class Request(val text: String, val interrupt: Boolean, val protected: Boolean)

    private val main = Handler(Looper.getMainLooper())
    private val pending = ArrayDeque<Request>()
    private var tts: TextToSpeech? = null
    private var ready = false
    private var failed = false
    private var rate = 1f

    /** ID of the protected (arrival) utterance that is still playing, or null. */
    @Volatile private var protectedId: String? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status -> main.post { onInit(status) } }
    }

    private fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) return fail("Speech could not start on this phone.")
        val language = engine.setLanguage(Locale.ENGLISH)
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            return fail("English speech is not available on this phone.")
        }
        engine.setSpeechRate(rate)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finished(utteranceId)
            @Deprecated("Required override")
            override fun onError(utteranceId: String?) = finished(utteranceId)
        })
        ready = true
        while (pending.isNotEmpty()) play(pending.removeFirst())
    }

    private fun fail(message: String) {
        failed = true
        pending.clear()
        onError(message)
    }

    private fun finished(id: String?) {
        if (id != null && id == protectedId) protectedId = null
    }

    override fun speak(text: String, interrupt: Boolean, protected: Boolean) {
        if (failed) return
        val request = Request(text, interrupt, protected)
        if (ready) play(request) else pending.addLast(request)
    }

    private fun play(r: Request) {
        val id = UUID.randomUUID().toString()
        val mode = if (r.interrupt && protectedId == null) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        if (r.protected) protectedId = id
        tts?.speak(r.text, mode, null, id)
    }

    override fun setRate(rate: Float) {
        this.rate = rate
        if (ready) tts?.setSpeechRate(rate)
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
