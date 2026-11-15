package com.genvision.nova.log

import java.io.File
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventLogTest {
    private fun tempDir() = kotlin.io.path.createTempDirectory("nova-log").toFile()
    private val fixed = { LocalDateTime.of(2026, 11, 20, 10, 15, 43, 870_000_000) }

    @Test fun writesTheFormatFromTheBrief() {
        val dir = tempDir()
        val log = EventLog(dir, 1_000_000, fixed)
        log.log("sighting", linkedMapOf("marker" to "0", "node" to "ENTRANCE_PLAZA"))
        log.log("speak", mapOf("text" to "You are at the entrance. Walk \"on\"."))
        assertEquals(
            listOf(
                "2026-11-20T10:15:43.870 sighting marker=0 node=ENTRANCE_PLAZA",
                "2026-11-20T10:15:43.870 speak text=\"You are at the entrance. Walk \\\"on\\\".\"",
            ),
            File(dir, "nova.log").readLines(),
        )
    }

    @Test fun eventWithoutFieldsHasNoTrailingSpace() {
        val log = EventLog(tempDir(), 1_000_000, fixed)
        log.log("app_start", mapOf("version" to "1"))
        log.log("help")
        assertEquals("2026-11-20T10:15:43.870 help", log.tail.value.last())
    }

    @Test fun rotatesWhenTheFileIsTooBig() {
        val dir = tempDir()
        val log = EventLog(dir, 100, fixed)
        repeat(10) { log.log("event", mapOf("n" to it.toString())) }
        assertTrue(File(dir, "nova.log.1").exists())
        assertTrue(File(dir, "nova.log").length() <= 200)
    }

    @Test fun tailKeepsOnlyTheLatestLines() {
        val log = EventLog(tempDir(), 1_000_000, fixed, tailSize = 3)
        repeat(5) { log.log("e", mapOf("n" to it.toString())) }
        assertEquals(3, log.tail.value.size)
        assertTrue(log.tail.value.last().endsWith("n=4"))
    }
}
