package com.vocabulary.app.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.QuizSource
import com.vocabulary.app.data.model.QuizViewStyle
import com.vocabulary.app.domain.QuizLimits
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.shortTitle
import com.vocabulary.app.ui.components.title
import com.vocabulary.app.ui.theme.Spacing

private fun QuizSource.label(): String = when (this) {
    QuizSource.ALL -> "All"
    QuizSource.FAVORITES -> "Favorites"
    QuizSource.MISTAKES -> "Mistakes"
    QuizSource.GROUP -> "Group"
}

private fun QuizSource.icon(): ImageVector = when (this) {
    QuizSource.ALL -> AppIcons.Cards
    QuizSource.FAVORITES -> AppIcons.Star
    QuizSource.MISTAKES -> AppIcons.AlertCircle
    QuizSource.GROUP -> AppIcons.List
}

private fun questionsText(n: Int) = if (n == 1) "1 question" else "$n questions"

@Composable
internal fun QuizSetup(
    ui: QuizSetupUi,
    onViewStyle: (QuizViewStyle) -> Unit,
    onSource: (QuizSource) -> Unit,
    onToggleMode: (QuizMode) -> Unit,
    onToggleAllModes: () -> Unit,
    onCount: (Int) -> Unit,
    onListMinutes: (Int) -> Unit,
    onStartStep: () -> Unit,
    onStartList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = ui.config
    val allSelected = ui.enabledModes.isNotEmpty() && ui.effectiveModes.size == ui.enabledModes.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Content),
        verticalArrangement = Arrangement.spacedBy(Spacing.Section)
    ) {
        // 1. Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Quiz", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            val cardView = config.viewStyle == QuizViewStyle.CARD
            IconButton(
                onClick = { onViewStyle(if (cardView) QuizViewStyle.LIST else QuizViewStyle.CARD) },
                modifier = Modifier.size(Spacing.Target)
            ) {
                Icon(
                    imageVector = if (cardView) AppIcons.List else AppIcons.Grid,
                    contentDescription = if (cardView) "Switch to list view" else "Switch to card view"
                )
            }
        }

        // 2. Source
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Compact)) {
            SetupLabel("Quiz from")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Compact)) {
                QuizSource.entries.filter { it != QuizSource.GROUP || ui.groupLabel != null }.forEach { source ->
                    SourceTile(
                        source = source,
                        count = ui.sourceCounts[source] ?: 0,
                        selected = config.source == source,
                        onClick = { onSource(source) }
                    )
                }
            }
        }

        // 3. Modes
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Compact)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SetupLabel("Mode · ${ui.effectiveModes.size} selected", Modifier.weight(1f))
                TextButton(
                    onClick = onToggleAllModes,
                    enabled = ui.enabledModes.isNotEmpty(),
                    modifier = Modifier.semantics {
                        contentDescription = if (allSelected) "Clear mode selection" else "Select all modes"
                    }
                ) { Text(if (allSelected) "Clear" else "Select all") }
            }
            if (config.viewStyle == QuizViewStyle.CARD) {
                // Real grid: two columns, four rows.
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Compact)) {
                    QuizMode.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Compact)
                        ) {
                            pair.forEach { mode ->
                                ModeCard(
                                    mode = mode,
                                    count = ui.perMode[mode] ?: 0,
                                    selected = mode in ui.effectiveModes,
                                    onToggle = { onToggleMode(mode) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    QuizMode.entries.forEachIndexed { i, mode ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ModeRow(
                            mode = mode,
                            count = ui.perMode[mode] ?: 0,
                            selected = mode in ui.effectiveModes,
                            onToggle = { onToggleMode(mode) }
                        )
                    }
                }
            }
        }

        // 4 + 5. Question number and list quiz time, side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Compact)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Compact)) {
                SetupLabel("Questions")
                StepperBox(
                    label = "Question number",
                    caption = null,
                    value = config.questionCount,
                    min = QuizLimits.MIN_COUNT,
                    max = QuizLimits.MAX_COUNT,
                    onValueChange = onCount,
                    compact = true
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Compact)) {
                SetupLabel("List time (min)")
                StepperBox(
                    label = "List Quiz time in minutes, 0 means no limit",
                    caption = null,
                    value = config.listTimeMinutes,
                    min = QuizLimits.MIN_MINUTES,
                    max = QuizLimits.MAX_MINUTES,
                    onValueChange = onListMinutes,
                    compact = true
                )
            }
        }

        // 6. Start buttons (one row, label only)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Standard)
        ) {
            Button(
                onClick = onStartStep,
                enabled = ui.canStart,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics { contentDescription = "Step Quiz" }
            ) { Text("Step Quiz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onStartList,
                enabled = ui.canStart,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(
                    2.dp,
                    if (ui.canStart) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics { contentDescription = "List Quiz" }
            ) { Text("List Quiz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
internal fun SetupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
private fun RowScope.SourceTile(source: QuizSource, count: Int, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 88.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "${source.label()}, $count items" },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Standard, horizontal = Spacing.Compact),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = source.icon(),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(source.label(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "$count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModeCard(
    mode: QuizMode,
    count: Int,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = count > 0
    val a11y = if (enabled) "${mode.title()}, ${questionsText(count)} available" else "${mode.title()}, not enough words yet"
    Card(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = a11y },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = Spacing.Standard, top = Spacing.Compact, bottom = Spacing.Compact, end = Spacing.Micro),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    mode.shortTitle(),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = if (enabled) (if (count == 1) "1 Item" else "$count Items") else "No words",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
        }
    }
}

@Composable
private fun ModeRow(mode: QuizMode, count: Int, selected: Boolean, onToggle: () -> Unit) {
    val enabled = count > 0
    val a11y = if (enabled) "${mode.title()}, ${questionsText(count)} available" else "${mode.title()}, not enough words yet"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = a11y }
            .heightIn(min = 52.dp)
            .padding(start = Spacing.Compact, end = Spacing.Content, top = Spacing.Compact, bottom = Spacing.Compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Compact)
    ) {
        Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
        Text(mode.title(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = if (enabled) "$count" else "Not enough words yet",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }
}
