package com.lokij.lokitunes.playback

import org.junit.Assert.*
import org.junit.Test

class AudioTuningTest {
    @Test fun defaultAndPresetCurves() {
        assertEquals(listOf(0, 0, 0, 0, 0), AudioTuning.curve("Flat", emptyList()))
        assertEquals(listOf(5, 3, 0, -1, -2), AudioTuning.curve("Bass", emptyList()))
        assertEquals("Flat", AudioTuning.sanitizePreset("not valid"))
    }

    @Test fun customSlidersAreBoundedAndStoredCorrectly() {
        assertEquals(listOf(6, -6, 0, 0, 0), AudioTuning.safeBands(listOf(99, -99)))
        assertEquals("6,-6,0,0,0", AudioTuning.encodeCustom(listOf(99, -99)))
        assertEquals(listOf(0, 3, 0, 0, 0), AudioTuning.decodeCustom("no,3,0,0,0"))
        assertEquals(listOf(2, 2, 2, 2, 2), AudioTuning.curve("Custom", listOf(2, 2, 2, 2, 2)))
    }

    @Test fun interpolationHandlesVendorBandFrequencies() {
        val curve = listOf(6, 3, 0, -3, -6)
        assertEquals(6.0, AudioTuning.gainAtHz(10.0, curve), 0.001)
        assertEquals(-6.0, AudioTuning.gainAtHz(20000.0, curve), 0.001)
        assertEquals(3.0, AudioTuning.gainAtHz(230.0, curve), 0.001)
        assertTrue(AudioTuning.gainAtHz(500.0, curve) in 0.0..3.0)
        assertEquals(0.0, AudioTuning.gainAtHz(1000.0, listOf()), 0.01)
    }

    @Test fun hardwareGainBoundsAndBassLimits() {
        val curve = listOf(6, 6, 6, 6, 6)
        assertEquals(300.toShort(), AudioTuning.bandMillibels(230.0, curve, -300, 300))
        assertEquals(0.toShort(), AudioTuning.bassStrength(-20))
        assertEquals(500.toShort(), AudioTuning.bassStrength(50))
        assertEquals(1000.toShort(), AudioTuning.bassStrength(200))
    }
}
