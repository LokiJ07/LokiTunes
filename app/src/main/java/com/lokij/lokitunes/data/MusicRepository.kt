package com.lokij.lokitunes.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.net.Uri

class MusicRepository(private val context: Context) {
    /** Reads Android's indexed local audio library. No broad storage access / network required. */
    fun scan(): List<MusicTrack> {
        val columns = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns += MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            columns += MediaStore.Audio.Media.DATA
        }

        val songs = mutableListOf<MusicTrack>()
        // EXTERNAL_CONTENT_URI represents indexed shared storage (including available volumes).
        val collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            collection,
            columns.toTypedArray(),
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            fun index(key: String) = cursor.getColumnIndexOrThrow(key)
            val idIdx = index(MediaStore.Audio.Media._ID)
            val titleIdx = index(MediaStore.Audio.Media.TITLE)
            val artistIdx = index(MediaStore.Audio.Media.ARTIST)
            val albumIdx = index(MediaStore.Audio.Media.ALBUM)
            val albumIdIdx = index(MediaStore.Audio.Media.ALBUM_ID)
            val durationIdx = index(MediaStore.Audio.Media.DURATION)
            val addedIdx = index(MediaStore.Audio.Media.DATE_ADDED)
            val pathIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                index(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                index(MediaStore.Audio.Media.DATA)
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val uri = ContentUris.withAppendedId(collection, id)
                val rawPath = cursor.getString(pathIdx).orEmpty()
                val folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MusicPaths.normalize(rawPath)
                } else {
                    MusicPaths.normalize(rawPath.substringBeforeLast('/', ""))
                }
                songs += MusicTrack(
                    id = uri.toString(),
                    uri = uri,
                    title = cursor.getString(titleIdx)?.takeUnless { it.isBlank() || it == "<unknown>" } ?: "Unknown track",
                    artist = cursor.getString(artistIdx)?.takeUnless { it.isBlank() || it == "<unknown>" } ?: "Unknown artist",
                    album = cursor.getString(albumIdx)?.takeUnless { it.isBlank() || it == "<unknown>" } ?: "Unknown album",
                    albumId = cursor.getLong(albumIdIdx),
                    durationMs = cursor.getLong(durationIdx).coerceAtLeast(0L),
                    folderPath = folder,
                    dateAdded = cursor.getLong(addedIdx)
                )
            }
        }
        return songs.distinctBy { it.id }
    }
}
