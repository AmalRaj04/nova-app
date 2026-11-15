package com.genvision.nova.core.voice

/**
 * Turns what the speech recogniser heard into a [VoiceCommand]. Pure text work, so it is tested
 * on a laptop. Recognisers write "D13" in many ways ("d 13", "d thirteen", "dee one three"),
 * so the text is squashed into one form before matching.
 *
 * @param destinationIds the IDs of the rooms in `map.json`, e.g. "D11", "D12", "D13".
 */
class VoiceCommandParser(private val destinationIds: List<String>) {

    /** The first command found in any of the recogniser's alternatives, or null if none. */
    fun parseFirst(alternatives: List<String>): VoiceCommand? =
        alternatives.firstNotNullOfOrNull { parse(it) }

    fun parse(heard: String): VoiceCommand? {
        val words = normalise(heard)
        if (words.isEmpty()) return null
        val spaced = words.joinToString(" ")

        // Control words win over room names: "stop going to D13" must stop.
        if (words.any { it in STOP_WORDS } || "end guidance" in spaced) return VoiceCommand.Stop
        if (words.any { it in REPEAT_WORDS } || "say again" in spaced || "once more" in spaced) return VoiceCommand.Repeat
        if ("where am" in spaced || "my location" in spaced || "which marker" in spaced) return VoiceCommand.WhereAmI
        if ("another" in spaced || "go home" in spaced || spaced == "home" || "main menu" in spaced) return VoiceCommand.ChooseAnother

        val destination = matchDestination(words)
        return destination?.let { VoiceCommand.GoTo(it) }
    }

    /** Lower case, letters and digits only, number words turned into digits, "dee" turned into "d". */
    private fun normalise(text: String): List<String> =
        text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(' ')
            .filter { it.isNotEmpty() }
            .map { NUMBER_WORDS[it] ?: if (it == "dee" || it == "de") "d" else it }

    private fun matchDestination(words: List<String>): String? {
        // "d 1 3", "d13" and "d thirteen" all squash to "d13".
        val squashed = words.joinToString("")
        val exact = destinationIds.filter { squashed.contains(it.lowercase()) }
        if (exact.size == 1) return exact.single()
        if (exact.size > 1) return null // heard two rooms: do not guess

        // "go to 13": digits alone, when exactly one room ends in those digits.
        val numbers = Regex("\\d+").findAll(squashed).map { it.value }.toSet()
        val byDigits = destinationIds.filter { id -> id.dropWhile { !it.isDigit() } in numbers }
        return byDigits.singleOrNull()
    }

    private companion object {
        val STOP_WORDS = setOf("stop", "cancel", "quit")
        val REPEAT_WORDS = setOf("repeat", "again")
        val NUMBER_WORDS = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
            "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
            "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
            "fourteen" to "14", "fifteen" to "15",
        )
    }
}
