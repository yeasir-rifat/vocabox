package com.vocabulary.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vocabulary.app.ui.theme.AppColors

/** Small tinted pill: green = easy, amber = medium, red = hard. Text keeps the semantic color for contrast. */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    val color = when (level) {
        1 -> AppColors.success
        2 -> AppColors.warning
        else -> AppColors.danger
    }
    TagBadge(text = levelLabel(level), color = color, modifier = modifier)
}
