package com.lokij.lokitunes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** App-private offline playlists. Existing Favorites are stored independently and untouched. */
class PlaylistsStore(context: Context) {
    private val prefs = context.getSharedPreferences("lokitunes_library", Context.MODE_PRIVATE)
    private val key = "playlists_json_v1"

    @Synchronized fun all(): List<SavedPlaylist> = runCatching {
        val stored = prefs.getString(key, "[]") ?: "[]"
        val array = JSONArray(stored)
        buildList {
            for (i in 0 until minOf(array.length(), PlaylistRules.MAX_PLAYLISTS)) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val name = PlaylistRules.cleanedName(item.optString("name"))
                if (!PlaylistRules.isValidName(name)) continue
                val trackIds = item.optJSONArray("trackIds") ?: JSONArray()
                val ids = buildList {
                    for (j in 0 until minOf(trackIds.length(), PlaylistRules.MAX_TRACKS_PER_PLAYLIST)) {
                        val value = trackIds.optString(j)
                        if (value.isNotBlank() && value !in this) add(value)
                    }
                }
                add(SavedPlaylist(id, name, ids))
            }
        }
    }.getOrDefault(emptyList())

    @Synchronized private fun save(playlists: List<SavedPlaylist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            array.put(JSONObject().apply {
                put("id", playlist.id)
                put("name", playlist.name)
                put("trackIds", JSONArray(playlist.trackIds))
            })
        }
        check(prefs.edit().putString(key, array.toString()).commit()) { "Could not save playlists" }
    }

    @Synchronized fun create(rawName: String): Boolean {
        val existing = all()
        if (!PlaylistRules.isValidName(rawName) || PlaylistRules.nameTaken(existing, rawName) ||
            existing.size >= PlaylistRules.MAX_PLAYLISTS) return false
        save(existing + SavedPlaylist(UUID.randomUUID().toString(), PlaylistRules.cleanedName(rawName), emptyList()))
        return true
    }

    @Synchronized fun rename(id: String, rawName: String): Boolean {
        val existing = all()
        if (!PlaylistRules.isValidName(rawName) || PlaylistRules.nameTaken(existing, rawName, id) ||
            existing.none { it.id == id }) return false
        save(existing.map { if (it.id == id) it.copy(name = PlaylistRules.cleanedName(rawName)) else it })
        return true
    }

    @Synchronized fun delete(id: String): Boolean {
        val current = all()
        if (current.none { it.id == id }) return false
        save(current.filterNot { it.id == id })
        return true
    }

    @Synchronized fun addTrack(id: String, trackId: String): Boolean {
        val current = all()
        val target = current.firstOrNull { it.id == id } ?: return false
        if (trackId.isBlank() || trackId in target.trackIds ||
            target.trackIds.size >= PlaylistRules.MAX_TRACKS_PER_PLAYLIST) return false
        save(current.map { if (it.id == id) it.copy(trackIds = PlaylistRules.appendUnique(it.trackIds, trackId)) else it })
        return true
    }

    @Synchronized fun removeTrack(id: String, trackId: String): Boolean {
        val current = all()
        if (current.none { it.id == id && trackId in it.trackIds }) return false
        save(current.map { if (it.id == id) it.copy(trackIds = it.trackIds.filterNot { song -> song == trackId }) else it })
        return true
    }
}
