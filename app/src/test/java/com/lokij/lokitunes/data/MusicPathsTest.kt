package com.lokij.lokitunes.data

import org.junit.Assert.*
import org.junit.Test

class MusicPathsTest {
    @Test fun `path normalization handles slashes and missing path`() {
        assertEquals("Music/OPM", MusicPaths.normalize("/Music/OPM/"))
        assertEquals("Other audio", MusicPaths.normalize(""))
        assertEquals("English", MusicPaths.folderLabel("Music/English/"))
    }
    @Test fun `explorer shows one nested level at a time`() {
        assertEquals("Music", MusicPaths.immediateChildFolder("", "Music/OPM"))
        assertEquals("Music/OPM", MusicPaths.immediateChildFolder("Music", "Music/OPM/Sad"))
        assertNull(MusicPaths.immediateChildFolder("Music", "Downloads/Songs"))
        assertTrue(MusicPaths.isDirectlyIn("Music/OPM", "Music/OPM"))
        assertFalse(MusicPaths.isDirectlyIn("Music", "Music/OPM"))
    }
}
