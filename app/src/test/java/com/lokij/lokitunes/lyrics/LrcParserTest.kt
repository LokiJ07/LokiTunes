package com.lokij.lokitunes.lyrics

import org.junit.Assert.*
import org.junit.Test

class LrcParserTest {
    @Test fun `parses two digit fractional timestamps and active line`() {
        val doc = LrcParser.parse("[00:01.50]Hello\n[00:03.00]World")
        assertTrue(doc.synchronized)
        assertEquals(1500L, doc.lines[0].startMs)
        assertEquals(-1, LrcParser.activeIndex(doc, 1499))
        assertEquals(0, LrcParser.activeIndex(doc, 1500))
        assertEquals(1, LrcParser.activeIndex(doc, 9000))
    }
    @Test fun `multiple timestamps copy same line and are sorted`() {
        val doc = LrcParser.parse("[00:05.500][00:02.250]Repeat")
        assertEquals(listOf(2250L, 5500L), doc.lines.map { it.startMs })
        assertEquals("Repeat", doc.lines[1].text)
    }
    @Test fun `offset and simple timed tags`() {
        val doc = LrcParser.parse("[offset:+300]\n[00:01]Line")
        assertEquals(1300L, doc.lines.single().startMs)
    }
    @Test fun `plain text and metadata`() {
        val doc = LrcParser.parse("[ar:Artist]\nFirst line\nSecond line")
        assertFalse(doc.synchronized)
        assertEquals(listOf("First line", "Second line"), doc.lines.map { it.text })
        assertEquals(-1, LrcParser.activeIndex(doc, 5000L))
    }
    @Test fun `invalid timestamps ignored`() {
        assertTrue(LrcParser.parse("[00:99]Bad").lines.isEmpty())
    }
}
