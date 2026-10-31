package com.lokij.lokitunes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkPaletteTest {
    @Test fun emptyOrGrayArtworkUsesFallback() {
        assertNull(bestArtworkAccent(intArrayOf()))
        assertNull(bestArtworkAccent(intArrayOf(0xFF666666.toInt(), 0xFF999999.toInt())))
    }
    @Test fun colorfulArtworkSelectsAccent() {
        assertEquals(0xFF8844CC.toInt(), bestArtworkAccent(intArrayOf(0xFF999999.toInt(), 0xFF8844CC.toInt())))
    }
    @Test fun ignoresTransparentPixels() {
        assertNull(bestArtworkAccent(intArrayOf(0x008844CC)))
    }
}
