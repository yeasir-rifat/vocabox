package com.vocabulary.app.ui.quiz

import androidx.compose.material3.FilledTonalIconButton
import com.vocabulary.app.ui.theme.markup
import com.vocabulary.app.ui.theme.TimesNewRoman
import com.vocabulary.app.ui.components.AppProgressBar
import com.vocabulary.app.ui.components.AppIcons
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.theme.AppColors
import com.vocabulary.app.ui.theme.bn
import androidx.compose.foundation.BorderStroke
import com.vocabulary.app.ui.quiz.report.ReportPreviewScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.domain.QuizQuestion
import com.vocabulary.app.ui.components.title
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.vocabulary.app.ui.components.BrandMark
import com.vocabulary.app.ui.components.ExamHeaderBar
import com.vocabulary.app.ui.components.ExamIconButton


@Composable
fun QuizScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val setup by viewModel.setup.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppPaused() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppResumed() }

    when (val s = state) {
        QuizState.Setup -> {
            val ui = setup
            if (ui == null) {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                QuizSetup(
                    ui = ui,
                    onViewStyle = viewModel::onViewStyle,
                    onSource = viewModel::onSource,
                    onToggleMode = viewModel::onToggleMode,
                    onToggleAllModes = viewModel::onToggleAllModes,
                    onCount = viewModel::onCount,
                    onListMinutes = viewModel::onListMinutes,
                    onStartStep = viewModel::startStep,
                    onStartList = viewModel::startList,
                    modifier = modifier
                )
            }
        }

        QuizState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        QuizState.Empty -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Not enough words for this quiz", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Choose another source or mode.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            Button(shape = MaterialTheme.shapes.small, onClick = viewModel::backToSetup) { Text("Back") }
        }

        is QuizState.Active -> QuizQuestionContent(
            state = s,
            onAnswer = viewModel::onAnswer,
            onNext = viewModel::onNext,
            onPrevious = viewModel::onPrevious,
            onSubmit = viewModel::submitStep,
            unansweredCount = viewModel::unansweredCount,
            modifier = modifier
        )

        is QuizState.ListActive -> ListQuizScreen(
            state = s,
            onSelect = viewModel::onSelectListAnswer,
            onSubmit = viewModel::submitList,
            modifier = modifier
        )

        is QuizState.Finished -> QuizResult(
            summary = s.summary,
            onRetry = viewModel::retry,
            onNewQuiz = viewModel::backToSetup,
            onDownload = viewModel::openReport,
            modifier = modifier
        )

        is QuizState.Report -> {
            val reportOptions by viewModel.reportOptions.collectAsStateWithLifecycle()
            ReportPreviewScreen(
                summary = s.summary,
                options = reportOptions,
                onOptionsChange = viewModel::onReportOptions,
                onBack = viewModel::closeReport,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun QuizQuestionContent(
    state: QuizState.Active,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit,
    unansweredCount: () -> Int,
    modifier: Modifier = Modifier
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    var unansweredNow by rememberSaveable { mutableStateOf(0) }
    val q = state.question
    val answered = state.selected != null
    val haptic = LocalHapticFeedback.current
    val wrongPick = answered && state.selected != q.correctIndex
    LaunchedEffect(state.index, wrongPick) {
        if (wrongPick) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // What the quiz is about (this question's category) and the countdown.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = q.mode.title(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!answered) {
                    val urgent = state.timeLeft <= 5
                    val timerColor = if (urgent) AppColors.danger else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(timerColor.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .semantics { contentDescription = "${state.timeLeft} seconds left" }
                    ) {
                        Text(
                            text = "${state.timeLeft}s",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = timerColor
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                // Submit: ends the quiz at any moment. Unanswered questions ask for a confirmation.
                Surface(
                    onClick = {
                        val left = unansweredCount()
                        if (left > 0) { unansweredNow = left; confirming = true } else onSubmit()
                    },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "Submit quiz" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Check, contentDescription = null)
                    }
                }
            }
            Text(
                text = "Question ${state.index + 1} of ${state.total}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppProgressBar(
                progress = (state.index + if (answered) 1 else 0) / state.total.toFloat(),
                label = "quizProgress"
            )
        }

        // One card: the question on top, the options under it as plain rows (no cards inside).
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (q.instruction.isNotBlank()) {
                        Text(
                            text = q.instruction,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = q.prompt.bn(),
                        fontFamily = TimesNewRoman,
                        style = if (q.prompt.length > 100) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 33.sp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (q.hint.isNotBlank()) {
                        Text(
                            text = q.hint.bn(),
                            fontFamily = TimesNewRoman,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    q.options.forEachIndexed { i, option ->
                        OptionRow(
                            label = ('A' + i).toString(),
                            text = option,
                            revealed = answered,
                            isCorrect = i == q.correctIndex,
                            isSelected = state.selected == i,
                            onClick = { onAnswer(i) }
                        )
                    }
                }
            }
        }

        // Previous and next question, side by side. Next opens once this question is answered.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = state.index > 0,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) { Text("Previous") }
            Button(
                onClick = onNext,
                enabled = answered && !state.isLast,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) { Text("Next") }
        }

        if (answered) {
            if (state.selected == TIMED_OUT) {
                Text(
                    text = "Time is up.",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.danger
                )
            }
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EXPLANATION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    q.explanation.lines().filter { it.isNotBlank() }.forEachIndexed { i, line ->
                        Text(
                            text = line.markup(),
                            fontFamily = TimesNewRoman,
                            style = if (i == 0) MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 30.sp)
                            else MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (state.isLast) {
                Button(
                    shape = MaterialTheme.shapes.small,
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("See result")
                }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Submit quiz?") },
            text = {
                Text("$unansweredNow of ${state.total} questions are unanswered and will count as wrong.")
            },
            confirmButton = {
                TextButton(onClick = { confirming = false; onSubmit() }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Keep answering") }
            }
        )
    }
}

@Composable
private fun OptionRow(
    label: String,
    text: String,
    revealed: Boolean,
    isCorrect: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Green for the correct option, red for a wrong pick: the letter circle and a light tint of the row.
    val fill = when {
        revealed && isCorrect -> AppColors.success
        revealed && isSelected -> AppColors.danger
        else -> null
    }
    val circleColor by animateColorAsState(
        targetValue = fill ?: Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "optionCircle"
    )
    val circleBorder by animateColorAsState(
        targetValue = fill ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 200),
        label = "optionCircleBorder"
    )
    val rowTint by animateColorAsState(
        targetValue = fill?.copy(alpha = 0.10f) ?: Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "optionTint"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowTint)
            .clickable(enabled = !revealed, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = when {
                    revealed && isCorrect -> "Option $label, $text, correct answer"
                    revealed && isSelected -> "Option $label, $text, your wrong answer"
                    else -> "Option $label, $text"
                }
            }
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // The circle matches the height of one line of text.
        Box(
            modifier = Modifier
                .size(OPTION_SIZE)
                .clip(CircleShape)
                .background(circleColor)
                .border(2.dp, circleBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = if (fill != null) Color.White else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text.bn(),
            fontFamily = TimesNewRoman,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = OPTION_SIZE.value.sp),
            modifier = Modifier.weight(1f)
        )
    }
}

/** Letter circle and one line of option text share this size. */
private val OPTION_SIZE = 28.dp

@Composable
private fun QuizResult(
    summary: QuizSummary,
    onRetry: () -> Unit,
    onNewQuiz: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (summary.total == 0) 0 else summary.correct * 100 / summary.total
    var showAll by rememberSaveable { mutableStateOf(false) }
    val skipped = summary.items.count { it.selected < 0 }
    val wrongAnswered = summary.total - summary.correct - skipped
    val attempted = summary.total - skipped
    val time = String.format("%02d:%02d", summary.durationSec / 60, summary.durationSec % 60)
    val subject = if (summary.modes.size == 1) summary.modes.first().title()
    else "Mixed quiz · ${summary.modes.size} modes"
    val topic = summary.groupLabel ?: "${summary.total} questions"
    val narrow = LocalConfiguration.current.screenWidthDp < 560

    Column(modifier = modifier.fillMaxSize()) {
        // Navbar: brand, page name, download.
        ExamHeaderBar {
            BrandMark()
            Text(
                text = "Vocabox",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = "Results & Analytics",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ExamIconButton(
                icon = AppIcons.Download,
                contentDescription = "Download",
                onClick = onDownload
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title block with the Download button.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        shape = MaterialTheme.shapes.small,
                        onClick = onDownload,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(36.dp)
                    ) { Text("Download") }
                }
            }

            // Score hero: ring with the percentage, marks and a one-line breakdown.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val body: @Composable () -> Unit = {
                        Column(
                            horizontalAlignment = if (narrow) Alignment.CenterHorizontally else Alignment.Start
                        ) {
                            Text(
                                text = "Obtained Marks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${summary.correct}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " / ${summary.total}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = "${summary.correct} correct · $wrongAnswered wrong · $skipped skipped · " +
                                    "$attempted Attempted · $time Time Used",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = if (narrow) TextAlign.Center else TextAlign.Start,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    if (narrow) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            ScoreRing(percent)
                            body()
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            ScoreRing(percent)
                            Box(modifier = Modifier.weight(1f)) { body() }
                        }
                    }
                }
            }

            // Answer sheet heading + the existing wrong / all filter.
            item {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Answer sheet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(${summary.total})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !showAll,
                            onClick = { showAll = false },
                            label = { Text("Wrong answers (${summary.wrong.size})") }
                        )
                        FilterChip(
                            selected = showAll,
                            onClick = { showAll = true },
                            label = { Text("All questions (${summary.total})") }
                        )
                    }
                }
            }
            if (!showAll && summary.wrong.isEmpty()) {
                item {
                    Text(
                        text = "Perfect score. Every answer was right.",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                val numbered = summary.items.mapIndexed { i, item -> (i + 1) to item }
                    .filter { showAll || !it.second.isCorrect }
                itemsIndexed(numbered) { _, (number, item) ->
                    ReviewCard(item, number, narrow)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    shape = MaterialTheme.shapes.small,
                    onClick = onNewQuiz,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("New quiz") }
                Button(
                    shape = MaterialTheme.shapes.small,
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) { Text("Try again") }
            }
        }
    }
}

/** Progress ring (128 dp, 10 dp stroke) that fills up to the percentage; the number sits inside. */
@Composable
private fun ScoreRing(percent: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(percent) {
        progress.animateTo(percent.coerceIn(0, 100) / 100f, tween(durationMillis = 700))
    }
    val track = MaterialTheme.colorScheme.outlineVariant
    val color = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * progress.value, false, topLeft, arc,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * One question of the answer sheet: number and question on top, the options as a plain grid
 * (one column on phones, two on wide screens) with a tick or cross on the right answer or the
 * wrong pick, then the explanation under a divider.
 */
@Composable
private fun ReviewCard(item: AnsweredQuestion, number: Int, narrow: Boolean) {
    val q: QuizQuestion = item.question
    val unanswered = item.selected < 0
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                            append("$number.  ")
                        }
                        append(listOf(q.instruction, q.prompt).filter { it.isNotBlank() }.joinToString(": ").bn())
                    },
                    fontFamily = TimesNewRoman,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (unanswered) {
                    val warn = AppColors.warning
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(warn.copy(alpha = 0.14f))
                            .border(1.dp, warn.copy(alpha = 0.4f), CircleShape)
                            .semantics { contentDescription = "Not answered" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.Help, contentDescription = null, tint = warn, modifier = Modifier.size(16.dp))
                    }
                }
            }

            val rows = if (narrow) q.options.indices.map { listOf(it) } else q.options.indices.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEach { i ->
                            ReviewOption(
                                letter = ('A' + i).toString(),
                                text = q.options[i],
                                isCorrect = i == q.correctIndex,
                                isWrongPick = i == item.selected && !item.isCorrect,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1 && !narrow) Spacer(Modifier.weight(1f))
                    }
                }
            }

            ExplanationBlock(q.explanation)
        }
    }
}

@Composable
private fun ReviewOption(
    letter: String,
    text: String,
    isCorrect: Boolean,
    isWrongPick: Boolean,
    modifier: Modifier = Modifier
) {
    val mark = when {
        isCorrect -> AppColors.success
        isWrongPick -> AppColors.danger
        else -> null
    }
    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .semantics {
                contentDescription = when {
                    isCorrect -> "Option $letter, $text, correct answer"
                    isWrongPick -> "Option $letter, $text, your wrong answer"
                    else -> "Option $letter, $text"
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(1.dp, mark?.copy(alpha = 0.6f) ?: MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = mark ?: MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = text.bn(),
            fontFamily = TimesNewRoman,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (mark != null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(mark.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCorrect) AppIcons.Check else AppIcons.Close,
                    contentDescription = null,
                    tint = mark,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/** Explanation: a divider, then the headword line first and the meaning, example, synonyms and antonyms. */
@Composable
private fun ExplanationBlock(explanation: String) {
    val lines = explanation.lines().filter { it.isNotBlank() }
    if (lines.isEmpty()) return
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        lines.forEachIndexed { i, line ->
            Text(
                text = line.markup(),
                fontFamily = TimesNewRoman,
                style = if (i == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
