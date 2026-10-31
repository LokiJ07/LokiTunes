package com.lokij.lokitunes.ui

import android.app.Application
import android.content.ComponentName
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


data class LokiUiState(
    val tracks: List<MusicTrack> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val lastPlayedId: String? = null,
    val error: String? = null
)

class LokiViewModel(application: Application) : AndroidViewModel(application) {
    private val library = MusicRepository(application)
    private val store = FavoritesStore(application)
    private val settings = application.getSharedPreferences("lokitunes_settings", 0)
    private val _state = MutableStateFlow(
        LokiUiState(
            favorites = store.all(),
            lastPlayedId = settings.getString("last_played_uri", null)
        )
    )
    val state: StateFlow<LokiUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingPlay: Pair<List<MusicTrack>, Int>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncPlaybackState(player)
        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = "Cannot play this file. Try another track.") }
        }
    }

    init {
        connect()
        viewModelScope.launch {
            while (isActive) {
                controller?.let { syncPlaybackState(it) }
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
                syncPlaybackState(connected)
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

    fun toggleFavorite(id: String) {
        _state.update { it.copy(favorites = store.toggle(id)) }
    }

    /** Playing a filtered list preserves that list as the playback queue. */
    fun playTracks(tracks: List<MusicTrack>, index: Int) {
        if (tracks.isEmpty() || index !in tracks.indices) return
        val player = controller
        if (player == null) {
            pendingPlay = tracks to index
            return
        }
        val items = tracks.map { track ->
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
        }
        player.setMediaItems(items, index, 0L)
        player.prepare()
        player.play()
        settings.edit().putString("last_played_uri", tracks[index].id).apply()
        _state.update { it.copy(lastPlayedId = tracks[index].id, error = null) }
        syncPlaybackState(player)
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

    private fun syncPlaybackState(player: Player) {
        val currentId = player.currentMediaItem?.mediaId
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0 } ?: 0L
        val last = _state.value.lastPlayedId
        if (currentId != null && currentId != last) {
            settings.edit().putString("last_played_uri", currentId).apply()
        }
        _state.update {
            it.copy(
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
