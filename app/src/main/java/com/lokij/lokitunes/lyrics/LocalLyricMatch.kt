package com.lokij.lokitunes.lyrics

import java.util.Locale

/** Pure filename/path matching rules. Never matches lyrics by artist/track similarity. */
object LocalLyricMatch {
    private fun normalized(path: String) = path.replace('\\', '/').trim('/').lowercase(Locale.ROOT)

    /** null means chosen tree isn't a parent of this music folder. */
    fun relativeFolders(musicFolder: String, treeRootId: String): List<String>? {
        val root = normalized(treeRootId.substringAfter(':', treeRootId))
        val folder = normalized(musicFolder)
        if (root == folder) return emptyList()
        if (root.isEmpty()) return folder.split('/').filter { it.isNotBlank() }
        if (folder.startsWith("$root/")) return folder.removePrefix("$root/").split('/').filter { it.isNotBlank() }
        return null
    }

    fun lyricFileNames(fileName: String, title: String): List<String> {
        val base = fileName.substringBeforeLast('.', fileName).trim().ifBlank { title.trim() }
        val names = listOf(base, title.trim()).filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
        return names.flatMap { listOf("$it.lrc", "$it.txt") }
    }

    /** Prefer a precise audio-file basename and timed LRC over a plain text match. */
    fun bestFile(available: List<String>, fileName: String, title: String): String? {
        val filenames = lyricFileNames(fileName, title)
        return filenames.firstNotNullOfOrNull { candidate ->
            available.firstOrNull { it.equals(candidate, ignoreCase = true) }
        }
    }
}
