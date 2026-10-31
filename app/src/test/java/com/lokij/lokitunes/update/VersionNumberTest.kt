package com.lokij.lokitunes.update

import org.junit.Assert.*
import org.junit.Test

class VersionNumberTest {
    @Test fun `parses release tag and installed version`() {
        assertEquals(VersionNumber(0, 3, 0), VersionNumber.parse("v0.3.0"))
        assertEquals(VersionNumber(0, 3, 1), VersionNumber.parse("v0.3.1"))
        assertEquals(VersionNumber(0, 3, 2), VersionNumber.parse("v0.3.2"))
        assertEquals(VersionNumber(0, 2, 0), VersionNumber.parse("0.2.0"))
    }

    @Test fun `only higher versions are considered upgrades`() {
        assertTrue(VersionNumber.parse("v0.3.1")!! > VersionNumber.parse("0.3.0")!!)
        assertTrue(VersionNumber.parse("v0.3.2")!! > VersionNumber.parse("v0.3.1")!!)
        assertFalse(VersionNumber.parse("v0.3.1")!! > VersionNumber.parse("v0.3.2")!!)
        assertFalse(VersionNumber.parse("v0.3.0")!! > VersionNumber.parse("0.3.1")!!)
        assertTrue(VersionNumber.parse("v1.0.0")!! > VersionNumber.parse("v0.99.99")!!)
        assertFalse(VersionNumber.parse("v0.2.9")!! > VersionNumber.parse("0.3.0")!!)
    }

    @Test fun `rejects unexpected tag formats`() {
        assertNull(VersionNumber.parse("draft"))
        assertNull(VersionNumber.parse("v0.3.0-rc1"))
        assertNull(VersionNumber.parse("v0.3"))
    }
}
