package com.lokij.lokitunes.data

import java.util.Locale

/** Persistent playlists refer to indexed MediaStore song IDs; they never copy or delete audio. */
data class SavedPlaylist(val id: String, val name: String, val trackIds: List<String>)

object PlaylistRules {
    const val MAX_PLAYLISTS = 100
    const val MAX_TRACKS_PER_PLAYLIST = 5000
    const val MAX_NAME_LENGTH = 60

    fun cleanedName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")
    fun isValidName(raw: String): Boolean {
        val normalized = cleanedName(raw)
        return normalized.isNotEmpty() && normalized.length <= MAX_NAME_LENGTH &&
            normalized.none { it.isISOControl() }
    }
    fun nameTaken(playlists: List<SavedPlaylist>, raw: String, exceptId: String? = null): Boolean =
        playlists.any { it.id != exceptId && it.name.lowercase(Locale.ROOT) == cleanedName(raw).lowercase(Locale.ROOT) }

    /** Appends a track once; playlist order is preserved. */
    fun appendUnique(ids: List<String>, id: String): List<String> =
        if (id.isBlank() || id in ids || ids.size >= MAX_TRACKS_PER_PLAYLIST) ids else ids + id
}
