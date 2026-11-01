package com.lokij.lokitunes.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SigningGuardTest {
    @Test fun sameSignerAllowed() {
        assertTrue(SigningGuard.sameCertificate(setOf("original-key"), setOf("original-key")))
    }

    @Test fun mismatchedSignerBlocked() {
        assertFalse(SigningGuard.sameCertificate(setOf("debug-key"), setOf("release-key")))
    }

    @Test fun missingSignersFailClosed() {
        assertFalse(SigningGuard.sameCertificate(emptySet(), setOf("release-key")))
        assertFalse(SigningGuard.sameCertificate(setOf("release-key"), emptySet()))
    }

    @Test fun subsetDoesNotPass() {
        assertFalse(SigningGuard.sameCertificate(setOf("A", "B"), setOf("A")))
    }
}
