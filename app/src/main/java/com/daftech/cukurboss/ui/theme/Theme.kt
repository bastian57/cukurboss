package com.daftech.cukurboss.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palet barbershop: charcoal gelap + aksen amber/gold. */
private val Charcoal = Color(0xFF141210)
private val CharcoalSurface = Color(0xFF1E1B16)
private val CharcoalVariant = Color(0xFF2A251D)
private val Amber = Color(0xFFFFB300)
private val Gold = Color(0xFFD4A017)
private val CreamText = Color(0xFFF5EFE3)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    secondary = Gold,
    onSecondary = Color.Black,
    tertiary = Color(0xFFE08E00),
    background = Charcoal,
    onBackground = CreamText,
    surface = CharcoalSurface,
    onSurface = CreamText,
    surfaceVariant = CharcoalVariant,
    onSurfaceVariant = CreamText,
    error = Color(0xFFFF6B6B),
    onError = Color.Black
)

@Composable
fun CukurBossTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
