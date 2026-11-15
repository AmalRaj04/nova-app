package com.genvision.nova

import com.genvision.nova.voice.DeepLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkTest {
    @Test fun readsTheRoomId() {
        assertEquals("D13", DeepLink.destinationId("nova://destination/D13"))
        assertEquals("D11", DeepLink.destinationId("NOVA://destination/D11/"))
    }

    @Test fun roundTrip() {
        assertEquals("D12", DeepLink.destinationId(DeepLink.forDestination("D12")))
    }

    @Test fun otherLinksAreIgnored() {
        assertNull(DeepLink.destinationId(null))
        assertNull(DeepLink.destinationId(""))
        assertNull(DeepLink.destinationId("https://example.com/destination/D13"))
        assertNull(DeepLink.destinationId("nova://other/D13"))
        assertNull(DeepLink.destinationId("nova://destination/"))
        assertNull(DeepLink.destinationId("nova://destination/D13?x=1"))
        assertNull(DeepLink.destinationId("nova://destination/a b"))
    }
}
