package com.lokij.lokitunes.data

import java.util.Locale
import kotlin.math.abs

/** Pure Kotlin library tools: no IO and no changes to users' audio files. */
data class IndexedSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val dateAdded: Long
)

enum class SongSort(val label: String) {
    TITLE("Title A–Z"), NEWEST("Recently added"), ARTIST("Artist A–Z"), ALBUM("Album A–Z"), DURATION("Longest first")
}

object LibraryInsights {
    fun sorted(songs: List<IndexedSong>, order: SongSort): List<IndexedSong> = when (order) {
        SongSort.TITLE -> songs.sortedWith(compareBy<IndexedSong> { it.title.lowercase(Locale.ROOT) }.thenBy { it.id })
        SongSort.NEWEST -> songs.sortedWith(compareByDescending<IndexedSong> { it.dateAdded }.thenBy { it.title.lowercase(Locale.ROOT) })
        SongSort.ARTIST -> songs.sortedWith(compareBy<IndexedSong> { it.artist.lowercase(Locale.ROOT) }.thenBy { it.title.lowercase(Locale.ROOT) })
        SongSort.ALBUM -> songs.sortedWith(compareBy<IndexedSong> { it.album.lowercase(Locale.ROOT) }.thenBy { it.title.lowercase(Locale.ROOT) })
        SongSort.DURATION -> songs.sortedWith(compareByDescending<IndexedSong> { it.durationMs }.thenBy { it.title.lowercase(Locale.ROOT) })
    }

    private fun normalize(s: String): String = s.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    /** Metadata-only candidates, NOT byte-for-byte file duplicates. Never removes any song. */
    fun potentialDuplicates(songs: List<IndexedSong>): List<List<IndexedSong>> =
        songs.filter { it.durationMs > 0L && normalize(it.title) !in setOf("", "unknown track") &&
            normalize(it.artist) !in setOf("", "unknown artist") }
            .groupBy { normalize(it.title) to normalize(it.artist) }
            .values.flatMap { sameName ->
                val remaining = sameName.sortedBy { it.durationMs }.toMutableList()
                buildList {
                    while (remaining.size > 1) {
                        val pivot = remaining.removeAt(0)
                        val matched = remaining.filter { abs(it.durationMs - pivot.durationMs) <= 2_000L }
                        if (matched.isNotEmpty()) {
                            remaining.removeAll(matched.toSet())
                            add(listOf(pivot) + matched)
                        }
                    }
                }
            }
            .sortedByDescending { it.size }
}
