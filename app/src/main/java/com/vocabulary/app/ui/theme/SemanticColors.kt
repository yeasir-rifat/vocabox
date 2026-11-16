package com.vocabulary.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Status colors that Material 3 does not provide. Light and dark values both meet text contrast. */
@Immutable
data class SemanticColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    /** Five activity levels for the heatmap (maroon ramp), from none to goal reached. */
    val heatmap: List<Color>
)

val LightSemanticColors = SemanticColors(
    success = Color(0xFF15803D),
    warning = Color(0xFF96590A),
    danger = Color(0xFFDC2626),
    heatmap = listOf(
        Color(0xFFEFE6DF), Color(0xFFF0C9C6), Color(0xFFD98E8B), Color(0xFFB85B59), Color(0xFF8A302F)
    )
)

val DarkSemanticColors = SemanticColors(
    success = Color(0xFF4ADE80),
    warning = Color(0xFFE8B04A),
    danger = Color(0xFFF87171),
    heatmap = listOf(
        Color(0xFF2A1F1E), Color(0xFF5A2A29), Color(0xFF8A3B3A), Color(0xFFC2605E), Color(0xFFE8938F)
    )
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

/** Use as AppColors.success, AppColors.warning, AppColors.danger inside composables. */
object AppColors {
    val success: Color
        @Composable @ReadOnlyComposable get() = LocalSemanticColors.current.success
    val warning: Color
        @Composable @ReadOnlyComposable get() = LocalSemanticColors.current.warning
    val danger: Color
        @Composable @ReadOnlyComposable get() = LocalSemanticColors.current.danger
}
