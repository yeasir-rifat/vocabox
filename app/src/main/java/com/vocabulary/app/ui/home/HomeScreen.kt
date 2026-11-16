package com.vocabulary.app.ui.home

import com.vocabulary.app.ui.theme.TimesNewRoman
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.theme.Spacing
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.components.shortPartOfSpeech
import com.vocabulary.app.ui.theme.bn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.DeckSource

@Composable
fun HomeScreen(
    onWordClick: (Long) -> Unit,
    onStartFlashcards: (DeckSource) -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Content, vertical = Spacing.Standard),
        verticalArrangement = Arrangement.spacedBy(Spacing.Content)
    ) {
        BrandHeader(onOpenStats = onOpenStats)

        val hero: @Composable () -> Unit = { HeroCard(state) }
        // Practice shortcuts sit right under the hero. Labels stay on one line.
        val shortcuts: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Standard)) {
                QuickAction(
                    icon = AppIcons.Cards,
                    label = "Practice",
                    caption = if (state.dueCount > 0) "${state.dueCount} due" else "All caught up",
                    onClick = { onStartFlashcards(DeckSource.SMART) },
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    icon = AppIcons.Pencil,
                    label = "Quiz",
                    onClick = onOpenQuiz,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    icon = AppIcons.AlertCircle,
                    label = "Mistakes",
                    caption = if (state.mistakeCount > 0) "${state.mistakeCount} words" else "None yet",
                    onClick = { onStartFlashcards(DeckSource.MISTAKES) },
                    enabled = state.mistakeCount > 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Wide screens (tablet, landscape): two columns. Phones: one stacked column.
        BoxWithConstraints {
            val wide = maxWidth >= 560.dp
            val daily: @Composable () -> Unit = {
                state.dailyWord?.let { word ->
                    DailyWordCard(word = word, onClick = { onWordClick(word.id) })
                }
            }
            val heatmap: @Composable () -> Unit = {
                ActivityHeatmapCard(activity = state.activity, goal = state.goal)
            }
            if (wide) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Content)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Content),
                        modifier = Modifier.height(IntrinsicSize.Max)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Content)) {
                            hero()
                            shortcuts()
                        }
                        Column(Modifier.weight(1f)) { daily() }
                    }
                    heatmap()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Content)) {
                    hero()
                    shortcuts()
                    daily()
                    heatmap()
                }
            }
        }
    }
}

/** Vocabox wordmark: a maroon serif "Aa" mark, the name, and the stats shortcut. */
@Composable
private fun BrandHeader(onOpenStats: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Standard)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            // Same white serif "Aa" as the app icon.
            Text(
                text = "Aa",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Vocabox",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onOpenStats) {
            Icon(AppIcons.BarChart, contentDescription = "Stats")
        }
    }
}

/** Circular progress ring that fills with the daily goal. */
@Composable
private fun ProgressRing(
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** Solid maroon hero: progress ring on the left, goal text and streak chip on the right. */
@Composable
private fun HeroCard(state: HomeUiState) {
    val goal = state.goal.coerceAtLeast(1)
    val remaining = (goal - state.todayCount).coerceAtLeast(0)
    val streak = state.streak
    val progress by animateFloatAsState(
        targetValue = (state.todayCount / goal.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "dailyProgress"
    )
    val onHero = MaterialTheme.colorScheme.onPrimary

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = onHero
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Card),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProgressRing(
                progress = progress,
                ringColor = onHero,
                trackColor = onHero.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(104.dp)
                    .semantics {
                        contentDescription = "${state.todayCount} of $goal cards done today"
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.todayCount}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of $goal",
                        style = MaterialTheme.typography.labelMedium,
                        color = onHero.copy(alpha = 0.85f)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (remaining == 0) "Goal reached" else "Today's goal",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (remaining == 0) {
                        "${state.todayCount} cards done. Nice work."
                    } else {
                        "$remaining more to " +
                            if (streak.current > 0) "keep your streak." else "start a streak."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onHero.copy(alpha = 0.9f)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .semantics {
                            contentDescription =
                                "Streak ${streak.current} days, longest ${streak.longest}"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Flame,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (streak.current == 1) "1 day" else "${streak.current} days",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "best ${streak.longest}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Editorial card: amber spine, large word, Bangla meaning as the second voice. */
@Composable
private fun DailyWordCard(word: WordEntity, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.Card),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WORD OF THE DAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val posColor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = buildAnnotatedString {
                        append(word.word)
                        val pos = word.partOfSpeech.shortPartOfSpeech()
                        if (pos.isNotEmpty()) {
                            append("  ")
                            withStyle(
                                SpanStyle(
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Normal,
                                    color = posColor
                                )
                            ) { append(pos) }
                        }
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = TimesNewRoman,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = word.banglaMeaning.bn(),
                    fontFamily = TimesNewRoman,
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 32.sp),
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Small line under the label, such as how many cards are due. */
    caption: String? = null
) {
    val contentAlpha = if (enabled) 1f else 0.5f
    AppCard(onClick = onClick, enabled = enabled, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(horizontal = 8.dp)
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
