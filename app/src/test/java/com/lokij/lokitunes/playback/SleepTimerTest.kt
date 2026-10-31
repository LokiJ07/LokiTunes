package com.lokij.lokitunes.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SleepTimerTest {
    @Test fun twentyMinutesAddsCorrectDuration() {
        assertEquals(1_201_234L, SleepTimer.deadline(1_234L, 20))
    }

    @Test fun expiryAndCancelReturnZero() {
        assertEquals(0L, SleepTimer.remainingMs(20_000L, 19_999L))
        assertEquals(0L, SleepTimer.remainingMs(20_000L, 0L))
        assertEquals(2_000L, SleepTimer.remainingMs(20_000L, 22_000L))
    }

    @Test fun invalidTimerDurationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { SleepTimer.deadline(0, 0) }
        assertThrows(IllegalArgumentException::class.java) { SleepTimer.deadline(0, 241) }
    }
}
