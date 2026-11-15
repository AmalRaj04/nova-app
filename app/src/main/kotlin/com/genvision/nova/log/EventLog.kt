package com.genvision.nova.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Local text log for field tests (FR-19). One line per event:
 * `2026-11-20T10:15:43.870 sighting marker=0 node=ENTRANCE_PLAZA`.
 * Only text is written. No camera images are ever stored.
 */
class EventLog(
    private val dir: File,
    private val maxBytes: Long,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
    private val tailSize: Int = 50,
) {
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    val file: File get() = File(dir, "nova.log")
    private val older: File get() = File(dir, "nova.log.1")

    private val _tail = MutableStateFlow<List<String>>(emptyList())
    /** The most recent lines, for the developer panel. */
    val tail: StateFlow<List<String>> = _tail

    @Synchronized
    fun log(event: String, fields: Map<String, String> = emptyMap()) {
        val line = buildString {
            append(now().format(stamp)).append(' ').append(event)
            for ((k, v) in fields) append(' ').append(k).append('=').append(format(v))
        }
        try {
            dir.mkdirs()
            if (file.length() >= maxBytes) { // rotate: keep one older file
                older.delete()
                file.renameTo(older)
            }
            file.appendText(line + "\n")
        } catch (_: Exception) {
            // Logging must never crash guidance. The line still reaches the on-screen tail.
        }
        _tail.value = (_tail.value + line).takeLast(tailSize)
    }

    /** Values with spaces, quotes or equals signs are put in quotes. */
    private fun format(value: String): String =
        if (value.any { it.isWhitespace() || it == '"' || it == '=' } || value.isEmpty()) {
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        } else value
}
