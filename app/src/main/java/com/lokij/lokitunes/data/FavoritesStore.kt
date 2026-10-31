package com.lokij.lokitunes.data

import android.content.Context

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("lokitunes_library", Context.MODE_PRIVATE)
    private val key = "favorite_track_uris"

    fun all(): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty().toSet()

    fun toggle(trackId: String): Set<String> {
        val next = all().toMutableSet()
        if (!next.add(trackId)) next.remove(trackId)
        prefs.edit().putStringSet(key, next).apply()
        return next
    }
}
