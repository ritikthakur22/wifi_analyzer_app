package com.rtwifianalyzer.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.rtwifianalyzer.app.domain.AppTheme

// taste-skill refined neutral palette:
// Chrome stays quiet and flat. Surfaces are 1 tone step apart. Data carries color.
private val Light = lightColorScheme(
    primary = Color(0xFF0F62FE),         // Tech Cobalt
    secondary = Color(0xFF0043CE),
    tertiary = Color(0xFF007D79),        // Deep Teal
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F4F8),
    background = Color(0xFFF8F9FA),      // Quiet neutral light
    onBackground = Color(0xFF161616),
    onSurface = Color(0xFF161616),
    onSurfaceVariant = Color(0xFF525252),
    outline = Color(0xFFE0E0E0)          // Crisp 1px structural border
)

private val Dark = darkColorScheme(
    primary = Color(0xFF4589FF),         // Crisp contrast on dark
    secondary = Color(0xFF78A9FF),
    tertiary = Color(0xFF00D2B4),        // Signal Teal
    surface = Color(0xFF12161F),         // Deep neutral surface
    surfaceVariant = Color(0xFF1B2230),
    background = Color(0xFF090D16),      // Deep Void background
    onBackground = Color(0xFFF4F4F4),
    onSurface = Color(0xFFF4F4F4),
    onSurfaceVariant = Color(0xFF8D8D8D),
    outline = Color(0xFF262E3D)          // Crisp 1px structural border on dark
)

@Composable
fun WifiAnalyzerTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val darkTheme = when (theme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        typography = Typography(),
        content = content
    )
}
