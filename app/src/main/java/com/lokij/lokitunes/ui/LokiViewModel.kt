package com.lokij.lokitunes.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.lokij.lokitunes.data.FavoritesStore
import com.lokij.lokitunes.data.MusicRepository
import com.lokij.lokitunes.data.MusicTrack
import com.lokij.lokitunes.playback.MusicPlaybackService
import com.lokij.lokitunes.playback.SleepTimer
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat


data class QueueEntry(val id: String, val title: String, val artist: String)

data class LokiUiState(
    val tracks: List<MusicTrack> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val favoriteSince: Map<String, Long> = emptyMap(),
    val queue: List<QueueEntry> = emptyList(),
    val queueIndex: Int = -1,
    val dynamicAlbumColors: Boolean = true,
    val sleepTimerEndMs: Long = 0L,
    val sleepRemainingMs: Long = 0L,
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val lastPlayedId: String? = null,
    val error: String? = null,
    val notice: String? = null
)

class LokiViewModel(application: Application) : AndroidViewModel(application) {
    private val library = MusicRepository(application)
    private val store = FavoritesStore(application)
    private val settings = application.getSharedPreferences("lokitunes_settings", 0)
    private val _state = MutableStateFlow(
        LokiUiState(
            favorites = store.all(),
            favoriteSince = store.favoriteDates(),
            dynamicAlbumColors = settings.getBoolean("dynamic_album_colors", true),
            sleepTimerEndMs = settings.getLong(SleepTimer.PREF_END_TIME, 0L),
            lastPlayedId = settings.getString("last_played_uri", null)
        )
    )
    val state: StateFlow<LokiUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingPlay: Pair<List<MusicTrack>, Int>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) =
            syncPlaybackState(player, events.contains(Player.EVENT_TIMELINE_CHANGED))
        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = "Cannot play this file. Try another track.") }
        }
    }

    init {
        connect()
        viewModelScope.launch {
            while (isActive) {
                controller?.let { syncPlaybackState(it) }
                val end = settings.getLong(SleepTimer.PREF_END_TIME, 0L)
                _state.update {
                    it.copy(sleepTimerEndMs = end,
                        sleepRemainingMs = SleepTimer.remainingMs(System.currentTimeMillis(), end))
                }
                delay(500)
            }
        }
    }

    private fun connect() {
        if (controllerFuture != null) return
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, MusicPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture !== future) return@addListener
            try {
                val connected = future.get()
                controller = connected
                connected.addListener(listener)
                _state.update { it.copy(isConnected = true, error = null) }
                syncPlaybackState(connected, true)
                pendingPlay?.let { (list, index) -> playTracks(list, index) }
                pendingPlay = null
            } catch (e: Exception) {
                _state.update { it.copy(error = "Music engine connection failed. Reopen LokiTunes.") }
            }
        }, ContextCompat.getMainExecutor(app))
    }

    fun refresh() {
        if (_state.value.isScanning) return
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, error = null) }
            val result = withContext(Dispatchers.IO) { runCatching { library.scan() } }
            result.onSuccess { scanned ->
                _state.update { it.copy(tracks = scanned, isScanning = false) }
            }.onFailure {
                _state.update { it.copy(isScanning = false, error = "Unable to read music files. Check audio permission.") }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun toggleFavorite(id: String) {
        _state.update { it.copy(favorites = store.toggle(id), favoriteSince = store.favoriteDates()) }
    }

    private fun mediaItemFor(track: MusicTrack): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build()
            ).build()

    /** Playing a filtered list preserves that list as the playback queue. */
    fun playTracks(tracks: List<MusicTrack>, index: Int) {
        if (tracks.isEmpty() || index !in tracks.indices) return
        val player = controller
        if (player == null) {
            pendingPlay = tracks to index
            return
        }
        val items = tracks.map(::mediaItemFor)
        player.setMediaItems(items, index, 0L)
        player.prepare()
        player.play()
        settings.edit().putString("last_played_uri", tracks[index].id).apply()
        _state.update { it.copy(lastPlayedId = tracks[index].id, error = null) }
        syncPlaybackState(player, true)
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() {
        controller?.let {
            if (it.currentPosition > 3_000L) it.seekTo(0L)
            else it.seekToPreviousMediaItem()
        }
    }
    fun seekTo(position: Long) { controller?.seekTo(position.coerceAtLeast(0L)) }
    /** Persist the album-art color setting, including across signed APK upgrades. */
    fun toggleDynamicAlbumColors() {
        val enabled = !_state.value.dynamicAlbumColors
        settings.edit().putBoolean("dynamic_album_colors", enabled).apply()
        _state.update { it.copy(dynamicAlbumColors = enabled) }
    }

    /** Jump relative to the real player position; never seek outside a known duration. */
    fun seekBy(deltaMs: Long) {
        controller?.let { player ->
            val maxDuration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0L }
            player.seekTo(boundedSeekPosition(player.currentPosition, deltaMs, maxDuration))
            syncPlaybackState(player)
        }
    }

    /** Add an existing library track to the active Media3 queue, preserving playback. */
    fun queueTrack(track: MusicTrack, playNext: Boolean) {
        val player = controller
        if (player == null) {
            _state.update { it.copy(notice = "Music player is still connecting. Try again.") }
            return
        }
        val item = mediaItemFor(track)
        val count = player.mediaItemCount
        val insertAt = queueInsertIndex(player.currentMediaItemIndex, count, playNext)
        player.addMediaItem(insertAt, item)
        if (count == 0) player.prepare() // Queue the track but do not start unexpectedly.
        syncPlaybackState(player, true)
        val notice = when {
            count == 0 -> "Added to queue. Press Play when ready."
            playNext && player.shuffleModeEnabled -> "Added after current song. Shuffle may change its playback order."
            playNext -> "Playing next: ${track.title}"
            else -> "Added to queue: ${track.title}"
        }
        _state.update { it.copy(notice = notice, error = null) }
    }

    fun clearUpcoming() {
        controller?.let { player ->
            val from = (player.currentMediaItemIndex + 1).coerceIn(0, player.mediaItemCount)
            if (from < player.mediaItemCount) {
                player.removeMediaItems(from, player.mediaItemCount)
                syncPlaybackState(player, true)
                _state.update { it.copy(notice = "Upcoming tracks cleared.") }
            }
        }
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; syncPlaybackState(it) }
    }
    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            syncPlaybackState(it)
        }
    }

    /** Queue edits apply to the active Media3 session, not just a visual list. */
    fun playQueueIndex(index: Int) {
        controller?.let { if (index in 0 until it.mediaItemCount) it.seekTo(index, 0L) }
    }

    fun moveQueueItem(from: Int, to: Int) {
        controller?.let {
            if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) {
                it.moveMediaItem(from, to)
                syncPlaybackState(it, true)
            }
        }
    }

    fun removeQueueItem(index: Int) {
        controller?.let {
            if (index in 0 until it.mediaItemCount) {
                it.removeMediaItem(index)
                syncPlaybackState(it, true)
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        val end = SleepTimer.deadline(System.currentTimeMillis(), minutes)
        settings.edit().putLong(SleepTimer.PREF_END_TIME, end).apply()
        _state.update { it.copy(sleepTimerEndMs = end,
            sleepRemainingMs = SleepTimer.remainingMs(System.currentTimeMillis(), end)) }
    }

    fun cancelSleepTimer() {
        settings.edit().remove(SleepTimer.PREF_END_TIME).apply()
        _state.update { it.copy(sleepTimerEndMs = 0L, sleepRemainingMs = 0L) }
    }

    /** Writes only favorite metadata: no audio files, credentials or account information. */
    fun backupFavorites(uri: Uri) {
        val snapshot = _state.value
        viewModelScope.launch(Dispatchers.IO) {
            val outcome = runCatching {
                val tracksById = snapshot.tracks.associateBy { it.id }
                val favorites = JSONArray()
                snapshot.favorites.forEach { id ->
                    val track = tracksById[id]
                    favorites.put(JSONObject().apply {
                        put("id", id)
                        put("title", track?.title.orEmpty())
                        put("artist", track?.artist.orEmpty())
                        put("folder", track?.folderPath.orEmpty())
                        put("durationMs", track?.durationMs ?: 0L)
                        put("savedAt", snapshot.favoriteSince[id] ?: 0L)
                    })
                }
                val document = JSONObject().apply {
                    put("app", "LokiTunes")
                    put("schema", 1)
                    put("favorites", favorites)
                }
                val stream = getApplication<Application>().contentResolver.openOutputStream(uri)
                    ?: error("Could not open destination")
                stream.bufferedWriter(Charsets.UTF_8).use { it.write(document.toString(2)) }
                favorites.length()
            }
            outcome.fold(
                onSuccess = { count -> _state.update { it.copy(notice = "Exported $count favorites. Keep the JSON file safe.", error = null) } },
                onFailure = { _state.update { it.copy(error = "Unable to export favorites. Try another folder.") } }
            )
        }
    }

    /** Imports known songs and merges with the current favorites. */
    fun restoreFavorites(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val outcome = runCatching {
                val resolver = getApplication<Application>().contentResolver
                val text = (resolver.openInputStream(uri) ?: error("Cannot open backup"))
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
                require(text.length <= 4_000_000) { "Backup is too large" }
                val document = JSONObject(text)
                require(document.optString("app") == "LokiTunes" && document.optInt("schema") == 1) {
                    "Not a supported LokiTunes favorites backup"
                }
                val items = document.getJSONArray("favorites")
                require(items.length() <= 20_000) { "Too many favorites" }
                val tracks = _state.value.tracks
                val byId = tracks.associateBy { it.id }
                val bySignature = tracks.groupBy { listOf(it.title, it.artist, it.folderPath, it.durationMs.toString()) }
                val matched = mutableMapOf<String, Long>()
                for (i in 0 until items.length()) {
                    val value = items.getJSONObject(i)
                    val savedId = value.optString("id")
                    val track = byId[savedId] ?: bySignature[listOf(
                        value.optString("title"), value.optString("artist"),
                        value.optString("folder"), value.optLong("durationMs").toString()
                    )]?.firstOrNull()
                    if (track != null) matched[track.id] = value.optLong("savedAt", 0L)
                }
                val added = store.merge(matched)
                val after = store.all()
                val dates = store.favoriteDates()
                _state.update { it.copy(favorites = after, favoriteSince = dates) }
                Triple(added, matched.size, items.length())
            }
            outcome.fold(
                onSuccess = { (added, matched, total) ->
                    _state.update { it.copy(notice = "Restored $added new favorites ($matched/$total songs matched).", error = null) }
                },
                onFailure = { _state.update { it.copy(error = "Cannot import this backup. Select a LokiTunes JSON export.") } }
            )
        }
    }

    private fun syncPlaybackState(player: Player, refreshQueue: Boolean = false) {
        val currentId = player.currentMediaItem?.mediaId
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0 } ?: 0L
        val last = _state.value.lastPlayedId
        if (currentId != null && currentId != last) {
            settings.edit().putString("last_played_uri", currentId).apply()
        }
        val queueSnapshot = if (refreshQueue || player.mediaItemCount != _state.value.queue.size) {
            (0 until player.mediaItemCount).map { index ->
                val item = player.getMediaItemAt(index)
                QueueEntry(item.mediaId,
                    item.mediaMetadata.title?.toString() ?: "Unknown track",
                    item.mediaMetadata.artist?.toString() ?: "Unknown artist")
            }
        } else _state.value.queue
        _state.update {
            it.copy(
                queue = queueSnapshot,
                queueIndex = player.currentMediaItemIndex,
                currentTrackId = currentId,
                isPlaying = player.isPlaying,
                durationMs = duration,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                shuffleEnabled = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                lastPlayedId = currentId ?: it.lastPlayedId
            )
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onCleared()
    }
}
