package com.lokij.lokitunes.data

import android.net.Uri

/** ID is a MediaStore content URI, not a filesystem path. */
data class MusicTrack(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val folderPath: String,
    val dateAdded: Long
) {
    val folderName: String get() = MusicPaths.folderLabel(folderPath)
}

/** Pure path helpers: MediaStore RELATIVE_PATH usually ends in '/'. */
object MusicPaths {
    fun normalize(path: String): String = path.replace('\\', '/').trim('/').let {
        if (it.isBlank()) "Other audio" else it
    }

    fun folderLabel(path: String): String = normalize(path).substringAfterLast('/')

    fun immediateChildFolder(parent: String, fullPath: String): String? {
        val normalized = normalize(fullPath)
        val base = if (parent.isBlank()) "" else "${normalize(parent)}/"
        if (!normalized.startsWith(base)) return null
        val remainder = normalized.removePrefix(base)
        if (remainder.isEmpty()) return null
        return base + remainder.substringBefore('/')
    }

    fun isDirectlyIn(parent: String, fullPath: String): Boolean =
        normalize(fullPath) == (if (parent.isBlank()) "Other audio" else normalize(parent))
}
