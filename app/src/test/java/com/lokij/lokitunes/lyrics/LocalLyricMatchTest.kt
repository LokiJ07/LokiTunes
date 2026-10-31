package com.lokij.lokitunes.lyrics

import org.junit.Assert.*
import org.junit.Test

class LocalLyricMatchTest {
    @Test fun folderLookup() {
        assertEquals(listOf("opm"), LocalLyricMatch.relativeFolders("Music/OPM", "primary:Music"))
        assertEquals(emptyList<String>(), LocalLyricMatch.relativeFolders("Music/OPM", "primary:Music/OPM"))
        assertNull(LocalLyricMatch.relativeFolders("Download", "primary:Music"))
    }
    @Test fun fileMatch() {
        assertEquals("My Song.lrc", LocalLyricMatch.bestFile(
            listOf("Other.txt", "My Song.txt", "My Song.lrc"), "My Song.mp3", "Wrong metadata"))
        assertEquals("Hello.txt", LocalLyricMatch.bestFile(listOf("Hello.txt"), "", "hello"))
        assertNull(LocalLyricMatch.bestFile(listOf("Something else.lrc"), "hello.mp3", "hello"))
    }
}
