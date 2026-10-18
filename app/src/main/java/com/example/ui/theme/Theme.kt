package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DotOrange,
    onPrimary = SurfaceBlack,
    primaryContainer = DotOrangeDark,
    onPrimaryContainer = TextPrimary,
    secondary = Player2Color,
    onSecondary = SurfaceBlack,
    tertiary = Player3Color,
    onTertiary = SurfaceBlack,
    background = SurfaceBlack,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderMuted,
    outlineVariant = BorderStrong
)

private val LightColorScheme = lightColorScheme(
    primary = DotOrange,
    onPrimary = SurfaceBlack,
    primaryContainer = DotOrangeLight,
    onPrimaryContainer = SurfaceBlack,
    secondary = Player2Color,
    onSecondary = SurfaceBlack,
    background = SurfaceBlack, // Consistent stylish dark-first game atmosphere
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderMuted,
    outlineVariant = BorderStrong
)

@Composable
fun DotBattleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SurfaceBlack.toArgb()
                window.navigationBarColor = SurfaceBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
