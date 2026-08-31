package com.example.spectroom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SpectRoomColors(
    val background: Color,
    val card: Color,
    val text: Color,
    val secondaryText: Color,
    val border: Color,
    val floatingPillBorder: Color,
    val isDark: Boolean,
)

val LocalSpectRoomColors = staticCompositionLocalOf {
    SpectRoomColors(
        background = AppBackgroundLight,
        card = AppCardLight,
        text = AppTextLight,
        secondaryText = AppSecondaryTextLight,
        border = Color.Black.copy(alpha = 0.08f),
        floatingPillBorder = Color.Black.copy(alpha = 0.10f),
        isDark = false,
    )
}

private val LightScheme = lightColorScheme(
    primary = TabSelected,
    onPrimary = Color.White,
    secondary = DistanceAccent,
    background = AppBackgroundLight,
    surface = AppCardLight,
    onBackground = AppTextLight,
    onSurface = AppTextLight,
    onSurfaceVariant = AppSecondaryTextLight,
)

private val DarkScheme = darkColorScheme(
    primary = TabSelected,
    onPrimary = Color.White,
    secondary = DistanceAccent,
    background = AppBackgroundDark,
    surface = AppCardDark,
    onBackground = AppTextDark,
    onSurface = AppTextDark,
    onSurfaceVariant = AppSecondaryTextDark,
)

@Composable
fun SpectRoomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        SpectRoomColors(
            background = AppBackgroundDark,
            card = AppCardDark,
            text = AppTextDark,
            secondaryText = AppSecondaryTextDark,
            border = Color.White.copy(alpha = 0.12f),
            floatingPillBorder = Color.White.copy(alpha = 0.28f),
            isDark = true,
        )
    } else {
        SpectRoomColors(
            background = AppBackgroundLight,
            card = AppCardLight,
            text = AppTextLight,
            secondaryText = AppSecondaryTextLight,
            border = Color.Black.copy(alpha = 0.08f),
            floatingPillBorder = Color.Black.copy(alpha = 0.10f),
            isDark = false,
        )
    }

    CompositionLocalProvider(LocalSpectRoomColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography,
            content = content,
        )
    }
}

object SpectRoomThemeTokens {
    val colors: SpectRoomColors
        @Composable get() = LocalSpectRoomColors.current
}
