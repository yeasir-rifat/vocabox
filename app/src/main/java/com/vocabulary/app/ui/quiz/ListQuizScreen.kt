package com.vocabulary.app.ui.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vocabulary.app.domain.QuizQuestion
import com.vocabulary.app.ui.components.AppProgressBar
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.theme.AppColors
import com.vocabulary.app.ui.theme.Spacing
import com.vocabulary.app.ui.theme.bn

private fun formatClock(totalSec: Int): String = "%02d:%02d".format(totalSec / 60, totalSec % 60)

/** All questions on one page. Correctness is not shown until the quiz is submitted. */
@Composable
internal fun ListQuizScreen(
    state: QuizState.ListActive,
    onSelect: (questionIndex: Int, option: Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val total = state.questions.size

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Content, vertical = Spacing.Standard),
            verticalArrangement = Arrangement.spacedBy(Spacing.Compact)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("List Quiz", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                state.timeLeftSec?.let { left ->
                    val low = left <= 30
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (low) AppColors.danger.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.semantics {
                            contentDescription = "Time left ${left / 60} minutes ${left % 60} seconds"
                        }
                    ) {
                        Text(
                            text = formatClock(left),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (low) AppColors.danger else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Text(
                text = "${state.answered} of $total answered",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AppProgressBar(
                progress = if (total == 0) 0f else state.answered / total.toFloat(),
                label = "listProgress"
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Spacing.Content, vertical = Spacing.Compact),
            verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
        ) {
            itemsIndexed(state.questions, key = { i, _ -> i }) { i, question ->
                ListQuestionCard(
                    number = i + 1,
                    question = question,
                    selected = state.answers[i],
                    onSelect = { option -> onSelect(i, option) }
                )
            }
        }

        // Bottom bar (sits above the navigation bar, which the root Scaffold draws below this screen)
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Content),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Micro)
            ) {
                Button(
                    onClick = { if (state.unanswered > 0) confirming = true else onSubmit() },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) { Text("Submit quiz") }
                Text(
                    text = "${state.unanswered} questions unanswered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Submit quiz?") },
            text = {
                Text(
                    "${state.unanswered} of $total questions are unanswered and will count as wrong."
                )
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
private fun ListQuestionCard(
    number: Int,
    question: QuizQuestion,
    selected: Int?,
    onSelect: (Int) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.Content),
            verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Standard)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = "Question $number" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$number",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (question.instruction.isNotBlank()) {
                        Text(
                            text = question.instruction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = question.prompt.bn(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (question.hint.isNotBlank()) {
                        Text(
                            text = question.hint.bn(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            question.options.forEachIndexed { i, option ->
                ListOptionRow(
                    label = ('A' + i).toString(),
                    text = option,
                    selected = selected == i,
                    onClick = { onSelect(i) }
                )
            }
        }
    }
}

@Composable
private fun ListOptionRow(label: String, text: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(150),
        label = "listOptionContainer"
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Option $label, $text" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.Content, vertical = Spacing.Standard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Standard)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text.bn(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
