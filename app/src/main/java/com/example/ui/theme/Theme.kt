package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NetCyanPrimary,
    onPrimary = Color(0xFF0A0F1D),
    primaryContainer = Color(0xFF004D59),
    onPrimaryContainer = Color(0xFFB5F4FF),
    secondary = NetTealAccent,
    onSecondary = Color(0xFF003831),
    secondaryContainer = Color(0xFF005147),
    onSecondaryContainer = Color(0xFF86FCE6),
    tertiary = NetGreenActive,
    background = NetNavyDark,
    onBackground = TextPrimaryDark,
    surface = NetCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NetSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = NetCardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = NetCyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAE6FD),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = NetTealAccentLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF16A34A),
    background = NetNavyLight,
    onBackground = TextPrimaryLight,
    surface = NetCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextSecondaryLight,
    outline = NetCardBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

