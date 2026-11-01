package com.lokij.lokitunes.playback

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the player so audio survives leaving the activity. Sleep timer is owned
 * here as well: destroying the UI/ViewModel does not cancel a running timer.
 */
@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {
    private val audioEffects by lazy { AudioEffectsController(settings) }
    private var servicePlayer: ExoPlayer? = null
    private val audioListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_AUDIO_SESSION_ID)) {
                audioEffects.attach(player.audioSessionId)
            }
        }
    }
    // A slider may produce dozens of setting changes per second. Coalesce hardware writes.
    private val updateAudioRunnable = Runnable {
        servicePlayer?.let { audioEffects.attach(it.audioSessionId) }
    }
    private val soundPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == AudioTuning.PREF_ENABLED || key == AudioTuning.PREF_PRESET ||
            key == AudioTuning.PREF_CUSTOM || key == AudioTuning.PREF_BASS) {
            handler.removeCallbacks(updateAudioRunnable)
            handler.postDelayed(updateAudioRunnable, 45L)
        }
    }

    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val settings: SharedPreferences by lazy {
        getSharedPreferences("lokitunes_settings", Context.MODE_PRIVATE)
    }
    private val timerListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SleepTimer.PREF_END_TIME) scheduleSleepTimer()
    }
    private val timerRunnable = object : Runnable {
        override fun run() {
            val end = settings.getLong(SleepTimer.PREF_END_TIME, 0L)
            val remaining = SleepTimer.remainingMs(System.currentTimeMillis(), end)
            if (end == 0L) return
            if (remaining == 0L) {
                mediaSession?.player?.pause()
                settings.edit().remove(SleepTimer.PREF_END_TIME).apply()
            } else {
                handler.postDelayed(this, remaining)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.setPlaybackSpeed(
            com.lokij.lokitunes.ui.SoundPresets.nearest(settings.getFloat("playback_speed", 1.0f))
        )
        servicePlayer = player
        player.addListener(audioListener)
        mediaSession = MediaSession.Builder(this, player).build()
        settings.registerOnSharedPreferenceChangeListener(soundPrefsListener)
        settings.registerOnSharedPreferenceChangeListener(timerListener)
        audioEffects.attach(player.audioSessionId)
        scheduleSleepTimer()
    }

    private fun scheduleSleepTimer() {
        handler.removeCallbacks(timerRunnable)
        val end = settings.getLong(SleepTimer.PREF_END_TIME, 0L)
        if (end > 0L) handler.post(timerRunnable)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        handler.removeCallbacks(timerRunnable)
        handler.removeCallbacks(updateAudioRunnable)
        settings.unregisterOnSharedPreferenceChangeListener(timerListener)
        settings.unregisterOnSharedPreferenceChangeListener(soundPrefsListener)
        servicePlayer?.removeListener(audioListener)
        audioEffects.release()
        servicePlayer = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
