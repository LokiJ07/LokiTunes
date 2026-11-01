package com.lokij.lokitunes.data

import org.junit.Assert.*
import org.junit.Test

class LibraryInsightsTest {
    private fun song(id: String, title: String = "My Song", artist: String = "Singer",
        duration: Long = 100_000L, added: Long = 1L) =
        IndexedSong(id, title, artist, "Album", duration, added)

    @Test fun `metadata duplicates ignore case and allow small duration difference`() {
        val groups = LibraryInsights.potentialDuplicates(listOf(
            song("a"), song("b", " my   SONG ", "SINGER", 101_000L),
            song("c", duration = 123_000L), song("d", title = "Another")
        ))
        assertEquals(1, groups.size)
        assertEquals(setOf("a", "b"), groups.first().map { it.id }.toSet())
    }

    @Test fun `unknown metadata and same title different artist are not flagged`() {
        assertTrue(LibraryInsights.potentialDuplicates(listOf(
            song("a", "Unknown track"), song("b", "Unknown track"),
            song("c", artist = "one"), song("d", artist = "two")
        )).isEmpty())
    }

    @Test fun `sort newest and by duration`() {
        val items = listOf(song("a", added = 1, duration = 2), song("b", added = 5, duration = 1))
        assertEquals("b", LibraryInsights.sorted(items, SongSort.NEWEST).first().id)
        assertEquals("a", LibraryInsights.sorted(items, SongSort.DURATION).first().id)
    }
}
