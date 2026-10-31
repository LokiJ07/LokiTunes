package com.lokij.lokitunes.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SoundPresetsTest {
    @Test fun `clamps unsupported values to nearest`() {
        assertEquals(1.0f, SoundPresets.nearest(0.99f))
        assertEquals(0.75f, SoundPresets.nearest(-10f))
        assertEquals(2.0f, SoundPresets.nearest(999f))
    }
}
