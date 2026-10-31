package com.lokij.lokitunes.ui

/** Chooses a colorful non-extreme pixel; monochrome or absent art keeps Midnight Aurora. */
internal fun bestArtworkAccent(pixels: IntArray): Int? {
    var winner: Int? = null
    var score = 0f
    for (argb in pixels) {
        val alpha = (argb ushr 24) and 255
        if (alpha < 220) continue
        val r = (argb ushr 16) and 255
        val g = (argb ushr 8) and 255
        val b = argb and 255
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        if (max == 0) continue
        val saturation = (max - min).toFloat() / max
        val brightness = max / 255f
        if (saturation < .24f || brightness < .22f || brightness > .94f) continue
        val candidateScore = saturation * (1f - kotlin.math.abs(brightness - .60f))
        if (candidateScore > score) {
            score = candidateScore
            winner = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return winner
}
