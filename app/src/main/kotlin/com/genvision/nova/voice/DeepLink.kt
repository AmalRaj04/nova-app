package com.genvision.nova.voice

/**
 * Links that start guidance, for the phone's voice assistant, shortcuts or a helper's tag:
 * `nova://destination/D13`. The room ID is checked against `map.json` by the caller.
 */
object DeepLink {
    const val SCHEME = "nova"
    const val HOST = "destination"

    /** The room ID in [uri], or null if it is not a NOVA destination link. */
    fun destinationId(uri: String?): String? {
        if (uri == null) return null
        val prefix = "$SCHEME://$HOST/"
        if (!uri.startsWith(prefix, ignoreCase = true)) return null
        val id = uri.substring(prefix.length).trim('/')
        return id.takeIf { it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c == '_' || c == '-' } }
    }

    fun forDestination(id: String) = "$SCHEME://$HOST/$id"
}
