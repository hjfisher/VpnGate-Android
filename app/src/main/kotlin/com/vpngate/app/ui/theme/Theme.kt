package com.vpngate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = SeaGreen,
    onPrimary = Color.Black,
    secondary = Teal,
    background = DarkBg,
    onBackground = Pearl,
    surface = CardDark,
    onSurface = Pearl,
    surfaceVariant = Color(0xFF242E3A),
    onSurfaceVariant = Color(0xFFB7C2CF),
    error = Danger
)

private val LightColors = lightColorScheme(
    primary = DarkTeal,
    onPrimary = Color.White,
    secondary = Teal,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626)
)

@Composable
fun VpnGateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}