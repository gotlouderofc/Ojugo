package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SeaBlue80,
    secondary = SeaBlueGrey80,
    tertiary = Orange80,
    background = OceanicDarkBg,
    surface = OceanicDarkCard,
    onPrimary = Color(0xFF00354B),
    onSecondary = Color(0xFF15333F),
    onTertiary = Color(0xFF562500),
    onBackground = Color(0xFFE1E2E5),
    onSurface = Color(0xFFE1E2E5),
    primaryContainer = SeaBluePrimary,
    onPrimaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SeaBluePrimary,
    secondary = SeaBlueSecondary,
    tertiary = AccentOrange,
    background = OceanicLightBg,
    surface = OceanicLightCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F1B25),
    onSurface = Color(0xFF0F1B25),
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E2E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force brand colors instead of system dynamic color
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
