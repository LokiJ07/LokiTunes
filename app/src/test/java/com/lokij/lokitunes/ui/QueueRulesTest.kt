package com.lokij.lokitunes.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueRulesTest {
    @Test fun playNextInsertsAfterCurrent() {
        assertEquals(4, queueInsertIndex(3, 9, true))
        assertEquals(9, queueInsertIndex(3, 9, false))
    }
    @Test fun emptyQueueCanBePopulated() {
        assertEquals(0, queueInsertIndex(-1, 0, true))
        assertEquals(0, queueInsertIndex(-1, 0, false))
    }
    @Test fun staleIndexCannotGoOutOfRange() {
        assertEquals(5, queueInsertIndex(999, 5, true))
        assertEquals(0, queueInsertIndex(-1, 5, true))
    }
    @Test fun relativeSeekStaysWithinKnownDuration() {
        assertEquals(0L, boundedSeekPosition(5_000L, -10_000L, 100_000L))
        assertEquals(100_000L, boundedSeekPosition(99_000L, 10_000L, 100_000L))
        assertEquals(20_000L, boundedSeekPosition(10_000L, 10_000L, null))
        assertEquals(0L, boundedSeekPosition(0L, -10_000L, null))
    }
    @Test fun seekHandlesOverflow() {
        assertEquals(Long.MAX_VALUE, boundedSeekPosition(Long.MAX_VALUE, 10_000L, null))
    }
}
