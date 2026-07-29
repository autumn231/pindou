package com.pindou.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Charcoal,
    primaryContainer = MintDark,
    onPrimaryContainer = Color.White,
    secondary = SoftPink,
    onSecondary = Charcoal,
    background = Background,
    onBackground = Charcoal,
    surface = Surface,
    onSurface = Charcoal,
    surfaceVariant = Cream,
    onSurfaceVariant = WarmBrown,
)

private val DarkColors = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Charcoal,
    secondary = SoftPink,
    onSecondary = Charcoal,
    background = Color(0xFF1A1F1D),
    onBackground = Color(0xFFE8E8E0),
    surface = Color(0xFF2A2F2D),
    onSurface = Color(0xFFE8E8E0),
)

@Composable
fun PindouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
