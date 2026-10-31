package com.lokij.lokitunes.playback

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the player so audio survives leaving the activity. Sleep timer is owned
 * here as well: destroying the UI/ViewModel does not cancel a running timer.
 */
class MusicPlaybackService : MediaSessionService() {
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
        mediaSession = MediaSession.Builder(this, player).build()
        settings.registerOnSharedPreferenceChangeListener(timerListener)
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
        settings.unregisterOnSharedPreferenceChangeListener(timerListener)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
