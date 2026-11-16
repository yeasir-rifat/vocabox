package com.vocabulary.app.ui.stats

import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.AppCard
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.local.entity.DailyActivityEntity
import com.vocabulary.app.domain.DayUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Current streak", "${state.streak.current}", "days", Modifier.weight(1f))
                StatTile("Longest streak", "${state.streak.longest}", "days", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Mastered", "${state.masteredCount}", "of ${state.totalWords} words", Modifier.weight(1f))
                StatTile("Learning", "${state.learningCount}", "words", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Accuracy", "${state.accuracyPercent}%", "all time", Modifier.weight(1f))
                StatTile("Answered", "${state.totalAnswered}", "cards", Modifier.weight(1f))
            }

            SectionCard("Streak calendar") { StreakCalendar(state) }
            SectionCard("Last 7 days (cards answered)") { WeeklyActivityChart(state) }
            SectionCard("Accuracy, last 14 days") { AccuracyChart(state) }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StreakCalendar(state: StatsUiState) {
    var monthOffset by rememberSaveable { mutableStateOf(0) }

    val month = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = month.get(Calendar.YEAR)
    val month0 = month.get(Calendar.MONTH)
    val firstDayOfWeek = month.firstDayOfWeek
    val leadingBlanks = (month.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val title = remember(monthOffset) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    }
    val weekdayLabels = remember(firstDayOfWeek) {
        val format = SimpleDateFormat("EEEEE", Locale.getDefault())
        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
        List(7) {
            val label = format.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            label
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthOffset-- }) {
                Icon(AppIcons.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { monthOffset++ }, enabled = monthOffset < 0) {
                Icon(AppIcons.ChevronRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val cells: List<Int?> = List(leadingBlanks) { null } + (1..daysInMonth).toList()
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val dayOfMonth = week.getOrNull(i)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayOfMonth != null) {
                            val epoch = DayUtils.epochDayOf(year, month0, dayOfMonth)
                            CalendarDay(
                                dayOfMonth = dayOfMonth,
                                activity = state.activity[epoch],
                                isToday = epoch == state.today
                            )
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(MaterialTheme.colorScheme.primary, "Goal met")
            LegendDot(MaterialTheme.colorScheme.primaryContainer, "Studied")
        }
    }
}

@Composable
private fun CalendarDay(dayOfMonth: Int, activity: DailyActivityEntity?, isToday: Boolean) {
    val (background, textColor) = when {
        activity?.goalMet == true ->
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        (activity?.reviewed ?: 0) > 0 ->
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> Color.Transparent to MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$dayOfMonth",
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun dayLabel(epochDay: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(DayUtils.noonMillis(epochDay)))

@Composable
private fun WeeklyActivityChart(state: StatsUiState) {
    val days = (state.today - 6..state.today).toList()
    val values = days.map { (state.activity[it]?.reviewed ?: 0).toFloat() }
    val metGoal = days.map { state.activity[it]?.goalMet == true }
    BarChart(
        values = values,
        labels = days.map { dayLabel(it, "EEE") },
        highlighted = metGoal,
        maxValue = maxOf(values.max(), state.goal.toFloat(), 1f),
        valueLabel = { "${it.toInt()}" }
    )
}

@Composable
private fun AccuracyChart(state: StatsUiState) {
    val days = (state.today - 13..state.today).toList()
    val values = days.map { day ->
        val a = state.activity[day]
        if (a == null || a.reviewed == 0) 0f else a.correct * 100f / a.reviewed
    }
    BarChart(
        values = values,
        labels = days.map { dayLabel(it, "d") },
        highlighted = values.map { it >= 80f },
        maxValue = 100f,
        valueLabel = { "${it.toInt()}" }
    )
}

/** Simple bar chart built from boxes. Zero values show as a thin stub. */
@Composable
private fun BarChart(
    values: List<Float>,
    labels: List<String>,
    highlighted: List<Boolean>,
    maxValue: Float,
    valueLabel: (Float) -> String,
    modifier: Modifier = Modifier
) {
    val barMaxHeight = 96.dp
    val strong = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val empty = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { i, value ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = if (value > 0f) valueLabel(value) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(if (value > 0f) barMaxHeight * (value / maxValue).coerceIn(0.04f, 1f) else 3.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            when {
                                value <= 0f -> empty
                                highlighted.getOrNull(i) == true -> strong
                                else -> soft
                            }
                        )
                )
                Text(
                    text = labels.getOrNull(i).orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
