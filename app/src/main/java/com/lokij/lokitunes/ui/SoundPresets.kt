package com.lokij.lokitunes.ui

/** Stable speed choices; we do not modify pitch separately. */
object SoundPresets {
    val values = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    fun nearest(requested: Float): Float =
        values.minByOrNull { kotlin.math.abs(it - requested) } ?: 1.0f
}
