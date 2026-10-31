package com.lokij.lokitunes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Midnight = Color(0xFF0B0C14)
val NightPanel = Color(0xFF171827)
val NightCard = Color(0xFF202135)
val Lavender = Color(0xFFB598FF)
val Mint = Color(0xFF60E8D8)
val SoftText = Color(0xFFA9ABC0)

private val Colors = darkColorScheme(
    primary = Lavender,
    secondary = Mint,
    tertiary = Color(0xFFEEABEE),
    background = Midnight,
    surface = NightPanel,
    surfaceVariant = NightCard,
    onBackground = Color(0xFFF9F7FF),
    onSurface = Color(0xFFF9F7FF),
    onSurfaceVariant = SoftText,
    onPrimary = Midnight
)

@Composable
fun LokiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
