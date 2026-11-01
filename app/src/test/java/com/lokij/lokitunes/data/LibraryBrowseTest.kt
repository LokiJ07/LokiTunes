package com.lokij.lokitunes.data

import org.junit.Assert.*
import org.junit.Test

class LibraryBrowseTest {
    @Test fun `search matches different metadata fields and multiple terms`() {
        assertTrue(LibraryBrowse.matches("night  drive", "Nightfall", "Drive Club", "Remix", "night.mp3", "Music/Remix"))
        assertTrue(LibraryBrowse.matches("MUSIC remix", "Song", "Singer", "Album", "song.mp3", "Music/Remix"))
        assertTrue(LibraryBrowse.matches("", "Song", "Singer", "Album", "song.mp3", "Music"))
        assertFalse(LibraryBrowse.matches("missing", "Song", "Singer", "Album", "song.mp3", "Music"))
    }

    @Test fun `counts each child under its parent and does not delete or modify music`() {
        val paths = listOf("Music/OPM", "Music/OPM/Old", "Music/English", "Bluetooth", "Music/English")
        val result = LibraryBrowse.recursiveFolderCounts(paths)
        assertEquals(4, result["Music"])
        assertEquals(2, result["Music/OPM"])
        assertEquals(1, result["Music/OPM/Old"])
        assertEquals(2, result["Music/English"])
        assertEquals(1, result["Bluetooth"])
        assertNull(result["Download"])
    }

    @Test fun `handles slashes and blank folder safely`() {
        assertEquals(mapOf("Music" to 1, "Music/OPM" to 1),
            LibraryBrowse.recursiveFolderCounts(listOf("/Music\\OPM/", "")))
    }
}
