package com.lokij.lokitunes.lyrics

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Query ONLY after user taps Search online; never automatically upload music metadata. */
data class RemoteLyricHit(
    val id: Long,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val synced: String?,
    val plain: String?
) {
    val bestText: String? get() = synced?.takeIf { it.isNotBlank() } ?: plain?.takeIf { it.isNotBlank() }
}

object LrcLibClient {
    fun search(title: String, artist: String): List<RemoteLyricHit> {
        require(title.isNotBlank() && title.length <= 120) { "Enter a song title (up to 120 characters)" }
        require(artist.length <= 120) { "Artist is too long" }
        val builder = Uri.parse("https://lrclib.net/api/search").buildUpon().appendQueryParameter("track_name", title.trim())
        if (artist.isNotBlank() && artist != "Unknown artist") builder.appendQueryParameter("artist_name", artist.trim())
        val conn = (URL(builder.build().toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LokiTunes/0.6.0 (https://github.com/LokiJ07/LokiTunes)")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 10_000
            readTimeout = 12_000
            instanceFollowRedirects = false
        }
        try {
            when (val status = conn.responseCode) {
                200 -> Unit
                429 -> throw IOException("LRCLIB is rate-limiting requests. Try again later.")
                else -> throw IOException("Lyrics search failed (HTTP $status)")
            }
            val response = conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val data = StringBuilder()
                val buf = CharArray(2048)
                while (true) {
                    val n = reader.read(buf)
                    if (n < 0) break
                    data.append(buf, 0, n)
                    if (data.length > 2_000_000) throw IOException("Server response too large")
                }
                data.toString()
            }
            val array = JSONArray(response)
            fun lyrics(row: JSONObject, key: String): String? =
                if (row.isNull(key)) null else row.optString(key, "").takeIf { it.isNotBlank() && it.length <= 200_000 }
            return (0 until minOf(array.length(), 20)).mapNotNull { index ->
                val row = array.optJSONObject(index) ?: return@mapNotNull null
                if (row.optBoolean("instrumental", false)) return@mapNotNull null
                val hit = RemoteLyricHit(
                    id = row.optLong("id", -1L),
                    title = row.optString("trackName", "Unknown track"),
                    artist = row.optString("artistName", "Unknown artist"),
                    durationSeconds = row.optDouble("duration", 0.0).toInt(),
                    synced = lyrics(row, "syncedLyrics"),
                    plain = lyrics(row, "plainLyrics")
                )
                hit.takeIf { it.bestText != null }
            }
        } finally { conn.disconnect() }
    }
}
