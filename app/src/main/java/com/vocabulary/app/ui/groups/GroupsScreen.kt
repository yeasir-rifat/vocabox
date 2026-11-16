package com.vocabulary.app.ui.groups

import com.vocabulary.app.ui.list.WordSheet
import com.vocabulary.app.data.model.WordView
import com.vocabulary.app.ui.theme.TimesNewRoman
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
            showAppearance = true,
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
    val progress = if (total == 0) 0f else readCount / total.toFloat()
    val done = total > 0 && readCount >= total
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.Card, vertical = Spacing.Content),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Content)
        ) {
            // Progress ring with the group number inside.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .semantics { contentDescription = "$readCount of $total read" },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp
                )
                if (done) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = if (group.isAppendix) "A" else "${group.number}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.Micro)
            ) {
                Text(
                    text = when {
                        group.isAppendix -> "Appendix"
                        group.range.isNotEmpty() -> group.range
                        else -> "Group ${group.number}"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (!group.isAppendix && group.range.isNotEmpty()) append("Group ${group.number} \u00B7 ")
                        append(if (total == 1) "1 word" else "$total words")
                        append(" \u00B7 $readCount read")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalIconButton(
                onClick = onQuiz,
                modifier = Modifier
                    .size(Spacing.Target)
                    .semantics { contentDescription = "Quiz ${group.title}" },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(AppIcons.Pencil, contentDescription = null)
            }
        }
    }
}

@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenWord: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val group by viewModel.group.collectAsStateWithLifecycle()
    val readIds by viewModel.readIds.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val categoryWords by viewModel.categoryWords.collectAsStateWithLifecycle()
    val wordView by viewModel.wordView.collectAsStateWithLifecycle()
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
                            viewModel.markRead(word.id)
                            // The Appearance setting decides how the word opens.
                            if (wordView == WordView.DETAILS) onOpenWord(word.id) else selectedId = word.id
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

            val dismissWord = {
                viewModel.stopSpeaking()
                selectedId = null
            }
            if (wordView == WordView.SHEET) {
                val sheetIndex = g.words.indexOfFirst { it.id == selectedId }
                if (sheetIndex >= 0) {
                    WordSheet(
                        words = g.words,
                        startIndex = sheetIndex,
                        bookmarkedIds = bookmarkedIds,
                        onToggleBookmark = viewModel::toggleBookmark,
                        onSpeak = viewModel::speak,
                        onWordShown = viewModel::markRead,
                        onDismiss = dismissWord
                    )
                }
            } else if (wordView == WordView.POPUP) {
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
                        onDismiss = dismissWord,
                        onSpeak = viewModel::speak
                    )
                }
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
                        fontFamily = TimesNewRoman,
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
                        fontFamily = TimesNewRoman,
                        maxLines = if (word.isPhrase()) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = word.banglaMeaningLine().bn(),
                        fontFamily = TimesNewRoman,
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
