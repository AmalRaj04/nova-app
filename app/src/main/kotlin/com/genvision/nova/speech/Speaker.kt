package com.genvision.nova.speech

/** Speech output. The app uses [AndroidSpeaker]; tests can use a fake. */
interface Speaker {
    /**
     * Speak [text]. [interrupt] false queues it after what is playing. While a [protected]
     * message (arrival) is playing, nothing interrupts it.
     */
    fun speak(text: String, interrupt: Boolean = true, protected: Boolean = false)
    /** Silence the speaker now, e.g. before the microphone listens. */
    fun stop()
    fun setRate(rate: Float)
    fun shutdown()
}
