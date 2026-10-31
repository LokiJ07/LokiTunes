package com.lokij.lokitunes.playback

/** Keeps timer math separate from Android APIs so the edge cases can be unit tested. */
object SleepTimer {
    const val PREF_END_TIME = "sleep_timer_ends_at"

    fun deadline(nowMs: Long, minutes: Int): Long {
        require(minutes in 1..240) { "Timer must be between 1 and 240 minutes" }
        return nowMs + minutes * 60_000L
    }

    fun remainingMs(nowMs: Long, deadlineMs: Long): Long =
        if (deadlineMs <= 0L) 0L else (deadlineMs - nowMs).coerceAtLeast(0L)
}
