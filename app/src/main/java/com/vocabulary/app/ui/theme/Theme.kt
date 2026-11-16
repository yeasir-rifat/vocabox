package com.vocabulary.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Restrained corner radii: 8dp controls, 12dp cards, 16dp for large surfaces.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = LightAccentSubtle,
    onPrimaryContainer = LightAccentStrong,
    secondary = LightAttention,
    onSecondary = Color.White,
    tertiaryContainer = LightAttentionContainer,
    onTertiaryContainer = LightAttention,
    // Used by the navigation indicator, so it follows the maroon accent.
    secondaryContainer = LightAccentSubtle,
    onSecondaryContainer = LightAccentStrong,
    background = LightCanvas,
    onBackground = LightFg,
    surface = LightCanvas,
    onSurface = LightFg,
    surfaceVariant = LightCanvasInset,
    onSurfaceVariant = LightFgMuted,
    outline = LightBorder,
    outlineVariant = LightBorderMuted,
    error = LightDanger,
    onError = Color.White,
    surfaceContainerLowest = LightCanvasSubtle,
    surfaceContainerLow = LightCanvasSubtle,
    surfaceContainer = LightCanvasSubtle,
    surfaceContainerHigh = LightCanvasInset,
    surfaceContainerHighest = LightCanvasSubtle
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkCanvas,
    primaryContainer = DarkAccentSubtle,
    onPrimaryContainer = DarkAccentStrong,
    secondary = DarkAttention,
    onSecondary = DarkCanvas,
    tertiaryContainer = DarkAttentionContainer,
    onTertiaryContainer = DarkAttention,
    secondaryContainer = DarkAccentSubtle,
    onSecondaryContainer = DarkAccentStrong,
    background = DarkCanvas,
    onBackground = DarkFg,
    surface = DarkCanvas,
    onSurface = DarkFg,
    surfaceVariant = DarkCanvasRaised,
    onSurfaceVariant = DarkFgMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorderMuted,
    error = DarkDanger,
    onError = DarkCanvas,
    surfaceContainerLowest = DarkCanvas,
    surfaceContainerLow = DarkCanvas,
    surfaceContainer = DarkCanvasSubtle,
    surfaceContainerHigh = DarkCanvasRaised,
    surfaceContainerHighest = DarkCanvasSubtle
)

/** Follows the system light/dark setting unless the user picked a theme in Settings. */
@Composable
fun VocabularyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSemanticColors provides if (darkTheme) DarkSemanticColors else LightSemanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
