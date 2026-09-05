package com.pablitosb.sportsbook.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = Color(0xFF052E16),
    primaryContainer = AccentGreenDim,
    onPrimaryContainer = AccentGreenSoft,
    secondary = AccentGreenSoft,
    onSecondary = Color(0xFF052E16),
    background = NavyBlack,
    onBackground = TextPrimary,
    surface = Navy,
    onSurface = TextPrimary,
    surfaceVariant = CardFill,
    onSurfaceVariant = TextMuted,
    outline = CardStroke,
    error = RegRed,
    onError = Color.White,
)

@Composable
fun PablitosSportsbookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = SportsbookTypography,
        content = content,
    )
}
