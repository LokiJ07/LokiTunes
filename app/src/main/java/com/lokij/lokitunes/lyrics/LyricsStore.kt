package com.lokij.lokitunes.lyrics

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.IOException

/** Remembers one SAF-selected .lrc/.txt document per MediaStore track URI. */
class LyricsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("lokitunes_lyrics_v1", Context.MODE_PRIVATE)
    fun savedUri(trackId: String): Uri? = prefs.getString(trackId, null)?.let(Uri::parse)

    fun attach(trackId: String, uri: Uri) {
        // SAF permission survives app restart and signed APK updates.
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.edit().putString(trackId, uri.toString()).apply()
    }

    fun clear(trackId: String) {
        val uri = savedUri(trackId)
        prefs.edit().remove(trackId).apply()
        if (uri != null) runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun load(uri: Uri): LyricDocument {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Could not open selected file")
        val maxChars = 200_000 // Reject huge/malformed selections without reading whole files into RAM.
        val text = stream.bufferedReader(Charsets.UTF_8).use { reader ->
            buildString {
                val chunk = CharArray(2_048)
                while (length < maxChars) {
                    val n = reader.read(chunk, 0, minOf(chunk.size, maxChars - length))
                    if (n < 0) break
                    append(chunk, 0, n)
                }
            }
        }
        return LrcParser.parse(text)
    }
}
