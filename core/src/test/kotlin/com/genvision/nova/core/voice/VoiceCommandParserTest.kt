package com.genvision.nova.core.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VoiceCommandParserTest {
    private val parser = VoiceCommandParser(listOf("D11", "D12", "D13"))

    private fun goTo(id: String) = VoiceCommand.GoTo(id)

    @Test fun roomNamesInManySpellings() {
        for (heard in listOf("go to D13", "Go to d13", "d 13", "D. 13", "go to d thirteen", "dee thirteen",
            "take me to D one three", "d1 3", "13", "go to 13", "the thirteen")) {
            assertEquals(goTo("D13"), parser.parse(heard), heard)
        }
        assertEquals(goTo("D11"), parser.parse("go to D eleven"))
        assertEquals(goTo("D12"), parser.parse("go to d twelve"))
        assertEquals(goTo("D12"), parser.parse("go to dee 12"))
    }

    @Test fun repeat() {
        for (heard in listOf("repeat", "Repeat that", "say again", "again", "once more")) {
            assertEquals(VoiceCommand.Repeat, parser.parse(heard), heard)
        }
    }

    @Test fun whereAmI() {
        for (heard in listOf("where am I", "Where am I?", "where am i now", "my location")) {
            assertEquals(VoiceCommand.WhereAmI, parser.parse(heard), heard)
        }
    }

    @Test fun stop() {
        for (heard in listOf("stop", "Stop guidance", "cancel", "end guidance")) {
            assertEquals(VoiceCommand.Stop, parser.parse(heard), heard)
        }
    }

    @Test fun chooseAnother() {
        for (heard in listOf("choose another", "another destination", "go home", "home")) {
            assertEquals(VoiceCommand.ChooseAnother, parser.parse(heard), heard)
        }
    }

    @Test fun stopWinsOverARoomName() {
        assertEquals(VoiceCommand.Stop, parser.parse("stop going to D13"))
    }

    @Test fun twoRoomsAreNotGuessed() {
        assertNull(parser.parse("D11 or D12"))
    }

    @Test fun unknownRoomOrNoiseIsNotUnderstood() {
        assertNull(parser.parse("go to D14"))
        assertNull(parser.parse("banana"))
        assertNull(parser.parse(""))
        assertNull(parser.parse("   "))
    }

    @Test fun firstUnderstoodAlternativeWins() {
        assertEquals(goTo("D12"), parser.parseFirst(listOf("go to the lamp", "go to D12", "go to D13")))
        assertNull(parser.parseFirst(listOf("hello", "banana")))
        assertNull(parser.parseFirst(emptyList()))
    }

    @Test fun roomIdsComeFromTheMapNotFromCode() {
        val other = VoiceCommandParser(listOf("A1", "B2"))
        assertEquals(VoiceCommand.GoTo("B2"), other.parse("go to B two"))
        assertNull(other.parse("go to D13"))
    }
}
