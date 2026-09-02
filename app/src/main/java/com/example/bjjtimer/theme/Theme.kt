package com.example.bjjtimer.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FightGreen,
    onPrimary = Color.Black,
    secondary = BeltGold,
    onSecondary = Color.Black,
    tertiary = RestRed,
    background = DarkBackground,
    surface = CardBackground,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun BJJTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
