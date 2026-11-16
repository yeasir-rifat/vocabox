package com.vocabulary.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vocabulary.app.domain.DayUtils
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.theme.LocalSemanticColors

private const val WEEKS = 18
private const val DAYS_IN_WEEK = 7

/** Maps cards answered on a day to a level 0..4 relative to the daily goal. */
private fun levelFor(count: Int, goal: Int): Int {
    if (count <= 0) return 0
    val ratio = count.toFloat() / goal.coerceAtLeast(1)
    return when {
        ratio >= 1f -> 4
        ratio >= 0.67f -> 3
        ratio >= 0.34f -> 2
        else -> 1
    }
}

/** Contribution-graph style grid of the last [WEEKS] weeks, one square per day (Sunday on top). */
@Composable
fun ActivityHeatmapCard(
    activity: Map<Long, Int>,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalSemanticColors.current.heatmap
    val today = remember { DayUtils.epochDay(System.currentTimeMillis()) }
    val activeDays = remember(activity) { activity.count { it.value > 0 } }

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (activeDays == 1) "1 active day" else "$activeDays active days",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(WEEKS / DAYS_IN_WEEK.toFloat())
                    .semantics { contentDescription = "Study activity over the last $WEEKS weeks" }
            ) {
                val gap = 3.dp.toPx()
                val cell = (size.width - gap * (WEEKS - 1)) / WEEKS
                val todayRow = Math.floorMod(today + 4, 7L).toInt() // epoch day 0 was a Thursday
                val firstDay = today - todayRow - (WEEKS - 1) * 7L
                for (col in 0 until WEEKS) {
                    for (row in 0 until DAYS_IN_WEEK) {
                        val day = firstDay + col * 7L + row
                        if (day > today) continue
                        drawRoundRect(
                            color = colors[levelFor(activity[day] ?: 0, goal)],
                            topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                }
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
