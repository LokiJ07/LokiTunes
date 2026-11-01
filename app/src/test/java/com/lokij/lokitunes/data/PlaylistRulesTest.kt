package com.lokij.lokitunes.data

import org.junit.Assert.*
import org.junit.Test

class PlaylistRulesTest {
    @Test fun `normalize names and reject duplicates`() {
        val playlist = SavedPlaylist("1", "Night Drive", emptyList())
        assertEquals("Night Drive", PlaylistRules.cleanedName("  Night   Drive "))
        assertTrue(PlaylistRules.nameTaken(listOf(playlist), "night drive"))
        assertFalse(PlaylistRules.nameTaken(listOf(playlist), "Night Drive", "1"))
        assertFalse(PlaylistRules.isValidName("  "))
        assertFalse(PlaylistRules.isValidName("x".repeat(61)))
    }
    @Test fun `append never duplicates existing song`() {
        assertEquals(listOf("a", "b"), PlaylistRules.appendUnique(listOf("a", "b"), "a"))
        assertEquals(listOf("a", "b", "c"), PlaylistRules.appendUnique(listOf("a", "b"), "c"))
    }
}
