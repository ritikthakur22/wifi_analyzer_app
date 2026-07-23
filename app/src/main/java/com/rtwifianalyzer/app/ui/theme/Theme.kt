package com.rtwifianalyzer.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.rtwifianalyzer.app.domain.AppTheme

private val Light = lightColorScheme(primary = Color(0xFF0069C9), secondary = Color(0xFF005EAA), tertiary = Color(0xFF00A6A6), surface = Color(0xFFF8FAFF), background = Color(0xFFF4F8FF))
private val Dark = darkColorScheme(primary = Color(0xFF9ACBFF), secondary = Color(0xFF81BFFF), tertiary = Color(0xFF61D6D6), surface = Color(0xFF111B2A), background = Color(0xFF08111F))
@Composable fun WifiAnalyzerTheme(theme: AppTheme, content: @Composable () -> Unit) { MaterialTheme(colorScheme = if (theme == AppTheme.DARK) Dark else if (theme == AppTheme.LIGHT) Light else if (isSystemInDarkTheme()) Dark else Light, typography = Typography(), content = content) }
