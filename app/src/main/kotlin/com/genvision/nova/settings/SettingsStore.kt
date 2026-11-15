package com.genvision.nova.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Saves [Settings] in SharedPreferences. Changes take effect on the next instruction. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings

    private fun read(): Settings {
        val d = Settings()
        return Settings(
            developerMode = prefs.getBoolean(KEY_DEV, d.developerMode),
            voiceCommands = prefs.getBoolean(KEY_VOICE, d.voiceCommands),
        ).withStepLength(prefs.getFloat(KEY_STEP, d.stepLengthM.toFloat()).toDouble())
            .withSpeechRate(prefs.getFloat(KEY_RATE, d.speechRate.toFloat()).toDouble())
    }

    fun update(change: (Settings) -> Settings) {
        val next = change(_settings.value)
        _settings.value = next
        prefs.edit()
            .putFloat(KEY_STEP, next.stepLengthM.toFloat())
            .putFloat(KEY_RATE, next.speechRate.toFloat())
            .putBoolean(KEY_DEV, next.developerMode)
            .putBoolean(KEY_VOICE, next.voiceCommands)
            .apply()
    }

    private companion object {
        const val KEY_STEP = "step_length_m"
        const val KEY_RATE = "speech_rate"
        const val KEY_DEV = "developer_mode"
        const val KEY_VOICE = "voice_commands"
    }
}
