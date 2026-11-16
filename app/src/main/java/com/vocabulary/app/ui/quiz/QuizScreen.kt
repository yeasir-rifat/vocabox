package com.vocabulary.app.ui.quiz

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
    modifier: Modifier = Modifier
) {
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Question ${state.index + 1} of ${state.total}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${state.timeLeft}s",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (state.timeLeft <= 5) AppColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { contentDescription = "${state.timeLeft} seconds left" }
                )
            }
            AppProgressBar(
                progress = (state.index + if (answered) 1 else 0) / state.total.toFloat(),
                label = "quizProgress"
            )
        }

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (q.instruction.isNotBlank()) {
                    Text(
                        text = q.instruction,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = q.prompt.bn(),
                    style = if (q.prompt.length > 100) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (q.hint.isNotBlank()) {
                    Text(
                        text = q.hint.bn(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }

        q.options.forEachIndexed { i, option ->
            OptionCard(
                label = ('A' + i).toString(),
                text = option,
                revealed = answered,
                isCorrect = i == q.correctIndex,
                isSelected = state.selected == i,
                onClick = { onAnswer(i) }
            )
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Explanation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    q.explanation.lines().filter { it.isNotBlank() }.forEachIndexed { i, line ->
                        Text(
                            text = line.bn(),
                            style = if (i == 0) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Button(
                shape = MaterialTheme.shapes.small,
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (state.isLast) "See result" else "Next")
            }
        }
    }
}

@Composable
private fun OptionCard(
    label: String,
    text: String,
    revealed: Boolean,
    isCorrect: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Only the letter circle is coloured: green for the correct option, red for a wrong pick.
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
    Card(
        onClick = onClick,
        enabled = !revealed,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .border(2.dp, circleBorder, CircleShape)
                    .semantics {
                        contentDescription = when {
                            revealed && isCorrect -> "Option $label, correct answer"
                            revealed && isSelected -> "Option $label, your wrong answer"
                            else -> "Option $label"
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (fill != null) Color.White else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = text.bn(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuizResult(
    summary: QuizSummary,
    onRetry: () -> Unit,
    onNewQuiz: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (summary.total == 0) 0 else summary.correct * 100 / summary.total
    val minutes = summary.durationSec / 60
    val seconds = summary.durationSec % 60
    var showAll by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (summary.modes.size == 1) summary.modes.first().title()
                            else "Mixed quiz · ${summary.modes.size} modes",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${summary.correct} / ${summary.total}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$percent% correct, ${minutes}m ${seconds}s",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
            if (!showAll && summary.wrong.isEmpty()) {
                item {
                    Text(
                        text = "Perfect score. Every answer was right.",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                itemsIndexed(if (showAll) summary.items else summary.wrong) { _, item ->
                    ReviewCard(item)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(AppIcons.Download, contentDescription = null)
                Text("  Download")
            }
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

@Composable
private fun ReviewCard(item: AnsweredQuestion) {
    val q: QuizQuestion = item.question
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = listOf(q.instruction, q.prompt).filter { it.isNotBlank() }.joinToString(": ").bn(),
                style = MaterialTheme.typography.titleMedium
            )
            if (item.isCorrect) {
                Text(
                    text = "Your answer: ${q.correctAnswer}".bn(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.success,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Your answer: ${item.selectedText ?: "No answer"}".bn(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.danger
                )
                Text(
                    text = "Correct: ${q.correctAnswer}".bn(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.success,
                    fontWeight = FontWeight.Medium
                )
            }
            ExplanationBlock(q.explanation)
        }
    }
}

/** Detailed explanation: headword line first, then meaning, example, synonyms and antonyms. */
@Composable
private fun ExplanationBlock(explanation: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Explanation",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        explanation.lines().filter { it.isNotBlank() }.forEachIndexed { i, line ->
            Text(
                text = line.bn(),
                style = if (i == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
