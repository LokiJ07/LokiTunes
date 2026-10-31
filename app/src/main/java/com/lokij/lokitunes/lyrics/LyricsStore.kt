package com.lokij.lokitunes.lyrics

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.lokij.lokitunes.data.MusicTrack
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** All lyrics are on-device except an explicitly user-requested LRCLIB search. */
data class ResolvedLyrics(val document: LyricDocument, val source: String)

class LyricsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("lokitunes_lyrics_v1", Context.MODE_PRIVATE)
    private val folderPrefs = context.getSharedPreferences("lokitunes_lyrics_folders_v1", Context.MODE_PRIVATE)
    fun savedUri(trackId: String): Uri? = prefs.getString(trackId, null)?.let(Uri::parse)
    fun savedFolder(): Uri? = folderPrefs.getString("tree", null)?.let(Uri::parse)

    fun attach(trackId: String, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        prefs.edit().putString(trackId, uri.toString()).remove("online_selected:$trackId").apply()
    }

    /** Grants access only to a user-selected folder, NOT to the whole phone. */
    fun setFolder(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        folderPrefs.edit().putString("tree", uri.toString()).apply()
    }

    fun clearFolder() {
        val old = savedFolder()
        folderPrefs.edit().remove("tree").apply()
        if (old != null) runCatching { context.contentResolver.releasePersistableUriPermission(old, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    fun clearManual(trackId: String) {
        val uri = savedUri(trackId)
        prefs.edit().remove(trackId).apply()
        if (uri != null) runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun clear(trackId: String) {
        clearManual(trackId)
        cacheFile(trackId).delete()
        prefs.edit().remove("online_selected:$trackId").apply()
    }

    fun load(uri: Uri): LyricDocument {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open lyric file")
        val reader = stream.bufferedReader(Charsets.UTF_8)
        return reader.use { LrcParser.parse(readBounded(it)) }
    }

    private fun readBounded(reader: java.io.Reader): String = buildString {
        val chunk = CharArray(2048)
        while (length <= MAX_CHARS) {
            val n = reader.read(chunk, 0, minOf(chunk.size, MAX_CHARS + 1 - length))
            if (n < 0) break
            append(chunk, 0, n)
        }
        if (length > MAX_CHARS) throw IOException("Lyric file is too large")
    }

    /** Priority: manually chosen document > automatic local folder match > offline downloaded cache. */
    fun resolve(track: MusicTrack): ResolvedLyrics? {
        savedUri(track.id)?.let { uri ->
            runCatching { load(uri) }.getOrNull()?.takeIf { it.lines.isNotEmpty() }?.let {
                return ResolvedLyrics(it, "Selected local file")
            }
        }
        // The user explicitly picked an online result: honor that selection over auto-match.
        if (prefs.getBoolean("online_selected:${track.id}", false)) {
            cachedLyrics(track.id)?.let { return it }
        }
        findLocalFile(track)?.let { uri ->
            runCatching { load(uri) }.getOrNull()?.takeIf { it.lines.isNotEmpty() }?.let {
                return ResolvedLyrics(it, "Auto-matched local .lrc/.txt")
            }
        }
        return cachedLyrics(track.id)
    }

    private fun cachedLyrics(trackId: String): ResolvedLyrics? {
        val cache = cacheFile(trackId)
        if (cache.isFile) runCatching {
            LrcParser.parse(cache.reader(Charsets.UTF_8).use(::readBounded))
        }.getOrNull()?.takeIf { it.lines.isNotEmpty() }?.let {
            return ResolvedLyrics(it, "Saved from LRCLIB · offline")
        }
        return null
    }

    /** Exact filename matching within the selected music tree / matching subfolder. No full-device scan. */
    private fun findLocalFile(track: MusicTrack): Uri? {
        val tree = savedFolder() ?: return null
        return runCatching {
            var dirId = DocumentsContract.getTreeDocumentId(tree)
            val subfolders = LocalLyricMatch.relativeFolders(track.folderPath, dirId)
            // A separate lyric folder is allowed: match an exact name in its root only.
            if (subfolders != null) {
                for (part in subfolders) {
                    val dir = children(tree, dirId).firstOrNull {
                        it.name.equals(part, true) && it.mime == DocumentsContract.Document.MIME_TYPE_DIR
                    } ?: return@runCatching null
                    dirId = dir.id
                }
            }
            val files = children(tree, dirId).filter { it.mime != DocumentsContract.Document.MIME_TYPE_DIR }
            val selectedName = LocalLyricMatch.bestFile(files.map { it.name }, track.fileName, track.title)
                ?: return@runCatching null
            val selected = files.first { it.name == selectedName }
            DocumentsContract.buildDocumentUriUsingTree(tree, selected.id)
        }.getOrNull()
    }

    private data class Child(val id: String, val name: String, val mime: String)

    private fun children(tree: Uri, parentId: String): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val cols = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        val result = mutableListOf<Child>()
        context.contentResolver.query(uri, cols, null, null, null)?.use { cursor ->
            while (cursor.moveToNext() && result.size < 1500) {
                result += Child(cursor.getString(0), cursor.getString(1).orEmpty(), cursor.getString(2).orEmpty())
            }
        }
        return result
    }

    /** Persist selected remote text inside private app storage. No write access to Music folder needed. */
    fun saveOnline(trackId: String, text: String) {
        require(text.isNotBlank() && text.length <= MAX_CHARS) { "Lyrics are empty or too large" }
        require(LrcParser.parse(text).lines.isNotEmpty()) { "No readable lyrics" }
        val dest = cacheFile(trackId)
        dest.parentFile?.mkdirs()
        val temp = File(dest.parentFile, dest.name + ".tmp")
        try {
            temp.writeText(text, Charsets.UTF_8)
            if (!temp.renameTo(dest)) {
                temp.copyTo(dest, overwrite = true)
                temp.delete()
            }
        } finally { temp.delete() }
        prefs.edit().putBoolean("online_selected:$trackId", true).apply()
    }

    private fun cacheFile(trackId: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(trackId.toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { "%02x".format(it) }
        return File(File(context.filesDir, "lyrics_cache"), "$name.txt")
    }

    companion object { private const val MAX_CHARS = 200_000 }
}
