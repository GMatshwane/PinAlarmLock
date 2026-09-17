package com.example.pinalarmlock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Navy,
        onPrimary = Color.White,
        secondary = AlarmOrange,
        onSecondary = Color.White,
        error = ErrorRed,
        background = Cream,
        surface = Color(0xFFFFFBFF),
    )

private val DarkColors =
    darkColorScheme(
        primary = NavyLight,
        secondary = AlarmOrangeLight,
        background = Color(0xFF12151C),
        surface = Color(0xFF1C212B),
    )

@Composable
fun PinAlarmLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
