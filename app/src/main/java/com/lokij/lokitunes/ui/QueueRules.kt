package com.lokij.lokitunes.ui

/** Pure helpers for JVM unit tests; no Android runtime required. */
internal fun queueInsertIndex(currentIndex: Int, itemCount: Int, playNext: Boolean): Int {
    require(itemCount >= 0)
    if (!playNext || itemCount == 0) return itemCount
    return (currentIndex + 1).coerceIn(0, itemCount)
}

internal fun boundedSeekPosition(position: Long, deltaMs: Long, durationMs: Long?): Long {
    val base = position.coerceAtLeast(0L)
    val target = if (deltaMs > 0L && base > Long.MAX_VALUE - deltaMs) Long.MAX_VALUE
        else if (deltaMs < 0L && base < Long.MIN_VALUE - deltaMs) Long.MIN_VALUE
        else base + deltaMs
    return if (durationMs != null && durationMs >= 0L) target.coerceIn(0L, durationMs)
        else target.coerceAtLeast(0L)
}
