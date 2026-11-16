package com.vocabulary.app.ui.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.GroupOrder
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.domain.WordGroup
import com.vocabulary.app.domain.prepositionSplit
import com.vocabulary.app.domain.relatedGroupVerbs
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.EmptyState
import com.vocabulary.app.ui.components.label
import com.vocabulary.app.ui.list.ListOptionsSidebar
import com.vocabulary.app.ui.list.ListSummaryRow
import com.vocabulary.app.ui.theme.Spacing
import com.vocabulary.app.ui.theme.bn

@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    onOpenGroup: (Int) -> Unit,
    onOpenQuiz: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val options by viewModel.options.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val readIds by viewModel.readIds.collectAsStateWithLifecycle()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.Compact, end = Spacing.Compact, top = Spacing.Compact),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, contentDescription = "Back") }
                Text(
                    text = "Groupwise",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { settingsOpen = true }) {
                    Icon(AppIcons.Settings, contentDescription = "List settings")
                }
            }

            ListSummaryRow(
                options = options,
                filters = filters,
                resultCount = state?.wordCount ?: 0,
                onClick = { settingsOpen = true },
                modifier = Modifier.padding(horizontal = Spacing.Content, vertical = Spacing.Compact)
            )

            val current = state
            when {
                current == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                current.groups.isEmpty() -> EmptyState(
                    icon = AppIcons.SearchOff,
                    text = if (options.isActive) "No words match these settings." else "No words yet."
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(Spacing.Content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
                ) {
                    items(current.groups, key = { it.index }) { group ->
                        GroupCard(
                            group = group,
                            readCount = group.words.count { it.id in readIds },
                            onOpen = { onOpenGroup(group.index) },
                            onQuiz = {
                                viewModel.quiz(group)
                                onOpenQuiz()
                            }
                        )
                    }
                }
            }
        }

        ListOptionsSidebar(
            visible = settingsOpen,
            options = options,
            filters = filters,
            resultCount = state?.wordCount ?: 0,
            availableLetters = state?.availableLetters ?: emptySet(),
            onToggleLetter = viewModel::toggleLetter,
            onClearLetters = viewModel::clearLetters,
            stripEnabled = false,
            onStripChange = {},
            showStripSwitch = false,
            onOptionsChange = viewModel::setOptions,
            onTypeSelected = viewModel::onTypeSelected,
            onLevelSelected = viewModel::onLevelSelected,
            onShuffleAgain = viewModel::shuffleAgain,
            onReset = viewModel::resetOptions,
            onClose = { settingsOpen = false }
        )
    }
}

@Composable
private fun GroupCard(group: WordGroup, readCount: Int, onOpen: () -> Unit, onQuiz: () -> Unit) {
    val total = group.words.size
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(
            modifier = Modifier.padding(Spacing.Content),
            verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (total == 1) "1 word" else "$total words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else readCount / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 6.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .semantics { contentDescription = "$readCount of $total read" },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$readCount / $total read",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onQuiz,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .heightIn(min = Spacing.Target)
                        .semantics { contentDescription = "Quiz ${group.title}" }
                ) { Text("Quiz", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onOpenQuiz: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val group by viewModel.group.collectAsStateWithLifecycle()
    val readIds by viewModel.readIds.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val categoryWords by viewModel.categoryWords.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.Compact, end = Spacing.Content, top = Spacing.Compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, contentDescription = "Back") }
            Text(
                text = group?.title ?: "Group",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val g = group
        if (g == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(Spacing.Content),
                verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
            ) {
                items(g.words, key = { it.id }) { word ->
                    WordListCard(
                        word = word,
                        read = word.id in readIds,
                        onClick = {
                            selectedId = word.id
                            viewModel.markRead(word.id)
                        }
                    )
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.quiz(g)
                        onOpenQuiz()
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Content)
                        .heightIn(min = 52.dp)
                ) { Text("Quiz this group (${g.words.size} words)") }
            }

            (g.words + categoryWords).firstOrNull { it.id == selectedId }?.let { word ->
                WordPopup(
                    word = word,
                    related = remember(word, categoryWords) { word.relatedGroupVerbs(categoryWords) },
                    onRelatedClick = { other ->
                        selectedId = other.id
                        viewModel.markRead(other.id)
                    },
                    isBookmarked = word.id in bookmarkedIds,
                    onToggleBookmark = { viewModel.toggleBookmark(word.id) },
                    onDismiss = {
                        viewModel.stopSpeaking()
                        selectedId = null
                    },
                    onSpeak = viewModel::speak
                )
            }
        }
    }
}

/** One word of the group as a card: word with short part of speech, and its Bangla meaning. */
@Composable
private fun WordListCard(word: WordEntity, read: Boolean, onClick: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = Spacing.Content, vertical = Spacing.Standard),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (word.isOneWord()) {
                    // Recall prompt: the phrase only. The word and its meaning are revealed in the popup.
                    Text(
                        text = word.englishMeaning,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    val split = word.prepositionSplit()
                    Text(
                        text = buildAnnotatedString {
                            // Preposition entries hide the preposition: "afraid ___".
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(if (split != null) split.first + " " else word.word)
                            }
                            if (split != null) {
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) { append("___") }
                            }
                            val pos = word.posLabel()
                            if (pos.isNotEmpty()) {
                                append("  ")
                                withStyle(
                                    SpanStyle(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) { append(pos) }
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (word.isPhrase()) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = word.banglaMeaningLine().bn(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (read) {
                Icon(
                    AppIcons.CheckCircle,
                    contentDescription = "Read",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
