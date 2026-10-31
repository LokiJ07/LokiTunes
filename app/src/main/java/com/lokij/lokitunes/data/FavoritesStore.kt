package com.lokij.lokitunes.data

import android.content.Context

/** Preferences names intentionally unchanged from v0.1 so existing favorites survive updates. */
class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("lokitunes_library", Context.MODE_PRIVATE)
    private val key = "favorite_track_uris"
    private fun dateKey(id: String) = "favorite_since:$id"

    fun all(): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty().toSet()

    fun favoriteDates(): Map<String, Long> = all().associateWith { id ->
        prefs.getLong(dateKey(id), 0L)
    }

    fun toggle(trackId: String): Set<String> {
        val next = all().toMutableSet()
        val edit = prefs.edit()
        if (next.add(trackId)) {
            edit.putLong(dateKey(trackId), System.currentTimeMillis())
        } else {
            next.remove(trackId)
            edit.remove(dateKey(trackId))
        }
        edit.putStringSet(key, next).apply()
        return next.toSet()
    }

    /** Merges backup entries instead of deleting already-saved favorites. */
    fun merge(entries: Map<String, Long>): Int {
        val next = all().toMutableSet()
        val edit = prefs.edit()
        var added = 0
        entries.forEach { (id, savedAt) ->
            if (next.add(id)) {
                added++
                edit.putLong(dateKey(id), savedAt.coerceAtLeast(0L))
            }
        }
        edit.putStringSet(key, next).apply()
        return added
    }
}
