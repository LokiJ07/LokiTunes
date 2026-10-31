package com.lokij.lokitunes.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/** Decoded art and its accent are produced together to avoid mismatched colors on fast skips. */
internal data class ArtworkVisual(val image: ImageBitmap, val accent: Color)

/** At most 144 bitmap reads on a background thread; no internet or third-party palette SDK. */
internal fun sampleArtworkAccent(bitmap: Bitmap): Color? {
    if (bitmap.width <= 0 || bitmap.height <= 0) return null
    val samples = IntArray(144)
    for (y in 0..11) for (x in 0..11) {
        val px = ((x + .5f) * bitmap.width / 12f).toInt().coerceIn(0, bitmap.width - 1)
        val py = ((y + .5f) * bitmap.height / 12f).toInt().coerceIn(0, bitmap.height - 1)
        samples[y * 12 + x] = bitmap.getPixel(px, py)
    }
    return bestArtworkAccent(samples)?.let { Color(it) }
}

