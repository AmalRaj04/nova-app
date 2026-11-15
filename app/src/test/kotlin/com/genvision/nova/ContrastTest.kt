package com.genvision.nova

import com.genvision.nova.ui.Palette
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/** Accessibility requirement 3: at least 7 to 1 for main text (WCAG relative luminance). */
class ContrastTest {
    private fun channel(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private fun ratio(a: Long, b: Long): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test fun whiteOnBlack() = assertTrue(ratio(Palette.WHITE, Palette.BLACK) >= 7.0)
    @Test fun blackOnYellowButtons() = assertTrue(ratio(Palette.BLACK, Palette.YELLOW) >= 7.0)
    @Test fun whiteOnDarkGreyButtons() = assertTrue(ratio(Palette.WHITE, Palette.DARK_GREY) >= 7.0)
    @Test fun whiteButtonBorderOnBlack() = assertTrue(ratio(Palette.WHITE, Palette.BLACK) >= 7.0)
    @Test fun greenMarkerBoxesOnBlack() = assertTrue(ratio(Palette.GREEN, Palette.BLACK) >= 7.0)
}
