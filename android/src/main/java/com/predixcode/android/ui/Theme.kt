package com.predixcode.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81B64C),
    onPrimary = Color(0xFF10140A),
    primaryContainer = Color(0xFF3E5A2A),
    onPrimaryContainer = Color(0xFFE6F5D5),

    secondary = Color(0xFF3A3A32),
    onSecondary = Color(0xFFE3E1D7),

    background = Color(0xFF262522),
    onBackground = Color(0xFFECECD0),

    surface = Color(0xFF2F2E29),
    onSurface = Color(0xFFECECD0),

    surfaceVariant = Color(0xFF3C3B34),
    onSurfaceVariant = Color(0xFFB8B6AA),

    outline = Color(0xFF4A4A3F),
)

/**
 * Shared board UI defaults (colors + piece theme).
 */
object BoardUiDefaults {
    // Piece sprite theme folder (matches desktop)
    const val PieceTheme: String = "neo/upscale"

    // Board background / tiles
    val AppBackground: Color = Color(0xFF262522)
    val LightSquare:   Color = Color(0xFFECECD0)
    val DarkSquare:    Color = Color(0xFF749552)

    // Highlights
    val SelectHighlight:   Color = Color(0xBFE1E16E)
    val LastMoveHighlight: Color = Color(0xBFC3CD5A)
    val TargetDot:         Color = Color(0x2A616161)
}

@Composable
fun Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}