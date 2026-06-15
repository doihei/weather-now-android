package com.doihei.weathernow.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// iOS の .preferredColorScheme / AppTheme に対応
// isSystemInDarkTheme() がシステムのダークモード設定を読む
// iOS の @Environment(\.colorScheme) に対応

private val LightColors = lightColorScheme() // Phase Ex でカスタムカラーに差し替える
private val DarkColors = darkColorScheme()

@Composable
fun WeatherNowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
