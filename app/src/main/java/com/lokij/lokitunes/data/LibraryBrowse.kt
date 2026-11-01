package com.lokij.lokitunes.data

import java.util.Locale

/** Small pure helpers so library processing can run off the Compose UI thread. */
object LibraryBrowse {
    /** All query terms may match across title, artist, album, file name and folder path. */
    fun matches(
        query: String,
        title: String,
        artist: String,
        album: String,
        fileName: String,
        folderPath: String
    ): Boolean {
        val tokens = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true
        val searchable = listOf(title, artist, album, fileName, folderPath)
            .joinToString(" ").lowercase(Locale.ROOT)
        return tokens.all { searchable.contains(it) }
    }

    /** Counts every track once for each ancestor folder, including its own folder. */
    fun recursiveFolderCounts(folderPaths: List<String>): Map<String, Int> {
        val counts = HashMap<String, Int>()
        folderPaths.forEach { rawPath ->
            val normalized = rawPath.replace('\\', '/').trim('/')
            if (normalized.isBlank()) return@forEach
            var ancestor = ""
            normalized.split('/').filter { it.isNotBlank() }.forEach { segment ->
                ancestor = if (ancestor.isEmpty()) segment else "$ancestor/$segment"
                counts[ancestor] = (counts[ancestor] ?: 0) + 1
            }
        }
        return counts
    }
}
