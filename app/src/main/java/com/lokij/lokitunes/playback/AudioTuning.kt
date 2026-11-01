package com.lokij.lokitunes.playback

import kotlin.math.ln
import kotlin.math.roundToInt

/** Device-independent EQ model, expressed in dB. Hardware adapters clamp to available ranges. */
object AudioTuning {
    const val PREF_ENABLED = "eq_enabled"
    const val PREF_PRESET = "eq_preset"
    const val PREF_CUSTOM = "eq_custom_bands"
    const val PREF_BASS = "eq_bass_percent"
    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
    val frequencies = listOf(60.0, 230.0, 910.0, 4000.0, 14000.0)
    val presets = listOf("Flat", "Bass", "Vocal", "Pop", "Rock", "Custom")
    private val profiles = mapOf(
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass" to listOf(5, 3, 0, -1, -2),
        "Vocal" to listOf(-2, -1, 2, 4, 2),
        "Pop" to listOf(2, 1, -1, 2, 3),
        "Rock" to listOf(4, 2, -1, 2, 4)
    )

    fun sanitizePreset(preset: String): String = preset.takeIf { it in presets } ?: "Flat"
    fun safeBands(bands: List<Int>): List<Int> = (0 until 5).map { bands.getOrNull(it)?.coerceIn(-6, 6) ?: 0 }
    fun encodeCustom(bands: List<Int>): String = safeBands(bands).joinToString(",")
    fun decodeCustom(serialized: String?): List<Int> = safeBands(serialized?.split(',')?.map { it.trim().toIntOrNull() ?: 0 } ?: emptyList())
    fun curve(preset: String, custom: List<Int>): List<Int> =
        if (sanitizePreset(preset) == "Custom") safeBands(custom) else profiles[sanitizePreset(preset)] ?: profiles.getValue("Flat")

    /** Interpolate in logarithmic frequency space to support different vendor band counts. */
    fun gainAtHz(freqHz: Double, curve: List<Int>): Double {
        val levels = safeBands(curve)
        if (freqHz <= frequencies.first() || !freqHz.isFinite()) return levels.first().toDouble()
        if (freqHz >= frequencies.last()) return levels.last().toDouble()
        val i = (0 until frequencies.lastIndex).first { freqHz <= frequencies[it + 1] }
        val fraction = (ln(freqHz) - ln(frequencies[i])) / (ln(frequencies[i + 1]) - ln(frequencies[i]))
        return levels[i] + (levels[i + 1] - levels[i]) * fraction
    }

    /** Converts dB to millibels; always within hardware-reported limits. */
    fun bandMillibels(freqHz: Double, curve: List<Int>, minMb: Int, maxMb: Int): Short =
        (gainAtHz(freqHz, curve) * 100.0).roundToInt()
            .coerceIn(minOf(minMb, maxMb), maxOf(minMb, maxMb)).toShort()

    fun bassStrength(percent: Int): Short = (percent.coerceIn(0, 100) * 10).toShort()
}
