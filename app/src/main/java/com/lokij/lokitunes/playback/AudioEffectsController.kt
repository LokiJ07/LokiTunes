package com.lokij.lokitunes.playback

import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

/**
 * Owns optional Android framework effects only for LokiTunes' current audio session.
 * Unsupported or unavailable effects never block music playback.
 * Must be called from the playback-service player thread (the main looper here).
 */
internal class AudioEffectsController(private val preferences: SharedPreferences) {
    private var sessionId: Int = -1
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0 || !preferences.getBoolean(AudioTuning.PREF_ENABLED, false)) {
            release()
            return
        }
        if (audioSessionId != sessionId) {
            release()
            sessionId = audioSessionId
            equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
            bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        }
        applyPreferences()
    }

    fun applyPreferences() {
        if (!preferences.getBoolean(AudioTuning.PREF_ENABLED, false)) {
            release()
            return
        }
        val chosen = AudioTuning.curve(
            preferences.getString(AudioTuning.PREF_PRESET, "Flat") ?: "Flat",
            AudioTuning.decodeCustom(preferences.getString(AudioTuning.PREF_CUSTOM, null))
        )
        equalizer?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange
                val bandCount = eq.numberOfBands.toInt()
                if (bandCount in 1..64 && range.size >= 2) {
                    for (band in 0 until bandCount) {
                        val freqHz = eq.getCenterFreq(band.toShort()) / 1000.0
                        eq.setBandLevel(band.toShort(), AudioTuning.bandMillibels(
                            freqHz, chosen, range[0].toInt(), range[1].toInt()
                        ))
                    }
                    eq.enabled = true
                }
            }.onFailure { runCatching { eq.enabled = false } }
        }
        bassBoost?.let { bass ->
            runCatching {
                val amount = preferences.getInt(AudioTuning.PREF_BASS, 0).coerceIn(0, 100)
                if (amount > 0) {
                    bass.setStrength(AudioTuning.bassStrength(amount))
                    bass.enabled = true
                } else {
                    bass.enabled = false
                }
            }.onFailure { runCatching { bass.enabled = false } }
        }
    }

    fun release() {
        equalizer?.let { runCatching { it.enabled = false }; runCatching { it.release() } }
        bassBoost?.let { runCatching { it.enabled = false }; runCatching { it.release() } }
        equalizer = null
        bassBoost = null
        sessionId = -1
    }
}
