package com.daftech.cukurboss.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** CukurBoss "Black Gold": hitam pekat + emas + putih. */
val BlackGold = Color(0xFFFFB300)
val BlackGoldDeep = Color(0xFFD4A017)
val BlackGoldSoft = Color(0xFFFFD54F)
val PureBlack = Color(0xFF000000)
val BlackSurface = Color(0xFF101010)
val BlackCard = Color(0xFF181818)
val BlackLine = Color(0xFF2A2A2A)
val PureWhite = Color(0xFFFFFFFF)
val MutedWhite = Color(0xFFB3B3B3)

private val DarkColors = darkColorScheme(
    primary = BlackGold,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFF3A2A00),
    onPrimaryContainer = BlackGoldSoft,
    secondary = BlackGoldDeep,
    onSecondary = PureBlack,
    tertiary = BlackGoldSoft,
    background = PureBlack,
    onBackground = PureWhite,
    surface = BlackSurface,
    onSurface = PureWhite,
    surfaceVariant = BlackCard,
    onSurfaceVariant = MutedWhite,
    surfaceContainerHighest = BlackLine,
    error = Color(0xFFFF6B6B),
    onError = PureBlack,
    outline = BlackLine
)

@Composable
fun CukurBossTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
