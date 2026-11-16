package com.vocabulary.app.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.DeckSource
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.domain.SavedSummary
import com.vocabulary.app.ui.components.ActionBar
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.EmptyState
import com.vocabulary.app.ui.components.SegmentedControl
import com.vocabulary.app.ui.components.SummaryCard
import com.vocabulary.app.ui.components.TagBadge
import com.vocabulary.app.ui.components.WordSummary
import com.vocabulary.app.ui.components.countText
import com.vocabulary.app.ui.components.shortLabel
import com.vocabulary.app.ui.list.WordSheet
import com.vocabulary.app.ui.theme.AppColors
import com.vocabulary.app.ui.theme.Spacing

private enum class SavedTab { FAVOURITES, MISTAKES }

/** One row of either list. [missed] is 0 for a favourite. */
private data class SavedEntry(val word: WordEntity, val missed: Int = 0)

@Composable
fun SavedScreen(
    onPractice: (DeckSource) -> Unit,
    onOpenQuiz: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = hiltViewModel()
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val mistakes by viewModel.mistakes.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    val tab = SavedTab.entries[tabIndex]

    val favouriteEntries = remember(bookmarks) { bookmarks?.map { SavedEntry(it) } }
    val mistakeEntries = remember(mistakes) { mistakes?.map { SavedEntry(it.word, it.count) } }

    // The word sheet works on a copy of the list taken when it opens, so unstarring a word inside
    // it does not pull the next word under your thumb.
    var sheetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var sheetIds by rememberSaveable { mutableStateOf(LongArray(0)) }
    var sheetWords by remember { mutableStateOf<List<WordEntity>>(emptyList()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    /** Runs [action] and offers Undo for a few seconds. */
    fun withUndo(message: String, action: () -> Unit, undo: () -> Unit) {
        action()
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Saved",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(
                    start = Spacing.Content,
                    end = Spacing.Content,
                    top = Spacing.Content,
                    bottom = Spacing.Compact
                )
            )
            SegmentedControl(
                options = SavedTab.entries,
                selected = tab,
                label = {
                    when (it) {
                        SavedTab.FAVOURITES -> "Favourites (${favouriteEntries?.size ?: 0})"
                        SavedTab.MISTAKES -> "Mistakes (${mistakeEntries?.size ?: 0})"
                    }
                },
                onSelect = { tabIndex = it.ordinal },
                modifier = Modifier.padding(horizontal = Spacing.Content, vertical = Spacing.Compact)
            )

            when (tab) {
                SavedTab.FAVOURITES -> SavedPane(
                    entries = favouriteEntries,
                    summaryIcon = AppIcons.StarFilled,
                    summaryTitle = { if (it == 1) "1 favourite" else "$it favourites" },
                    emptyIcon = AppIcons.Star,
                    emptyText = "No favourites yet. Tap the star on a word to save it.",
                    actionIcon = AppIcons.StarFilled,
                    actionTint = MaterialTheme.colorScheme.secondary,
                    actionDescription = "Remove from favourites",
                    onAction = { id ->
                        withUndo(
                            message = "Removed from favourites",
                            action = { viewModel.removeBookmark(id) },
                            undo = { viewModel.toggleBookmark(id) }
                        )
                    },
                    onOpen = { words, index ->
                        sheetWords = words
                        sheetIds = words.map { it.id }.toLongArray()
                        sheetIndex = index
                    },
                    onQuiz = {
                        viewModel.quizFavorites()
                        onOpenQuiz()
                    },
                    onFlashcards = { onPractice(DeckSource.BOOKMARKED) }
                )

                SavedTab.MISTAKES -> SavedPane(
                    entries = mistakeEntries,
                    summaryIcon = AppIcons.AlertCircle,
                    summaryTitle = { if (it == 1) "1 mistake" else "$it mistakes" },
                    emptyIcon = AppIcons.CheckCircle,
                    emptyText = "No mistakes. Words you get wrong in a quiz will appear here.",
                    actionIcon = AppIcons.Close,
                    actionTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionDescription = "Dismiss this mistake",
                    onAction = { id ->
                        val count = mistakes?.firstOrNull { it.word.id == id }?.count ?: 1
                        withUndo(
                            message = "Mistake dismissed",
                            action = { viewModel.removeMistake(id) },
                            undo = { viewModel.restoreMistake(id, count) }
                        )
                    },
                    onOpen = { words, index ->
                        sheetWords = words
                        sheetIds = words.map { it.id }.toLongArray()
                        sheetIndex = index
                    },
                    onQuiz = {
                        viewModel.quizMistakes()
                        onOpenQuiz()
                    },
                    onFlashcards = { onPractice(DeckSource.MISTAKES) }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        )

        val openIndex = sheetIndex
        if (openIndex != null) {
            // After a rotation the copy is gone; rebuild it from the saved ids once the lists have loaded.
            val words = if (sheetWords.isNotEmpty()) {
                sheetWords
            } else {
                val pool = (bookmarks.orEmpty() + mistakes.orEmpty().map { it.word }).associateBy { it.id }
                sheetIds.mapNotNull { pool[it] }
            }
            val loaded = bookmarks != null && mistakes != null
            when {
                words.isNotEmpty() -> WordSheet(
                    words = words,
                    startIndex = openIndex,
                    bookmarkedIds = bookmarkedIds,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onSpeak = viewModel::speak,
                    onWordShown = viewModel::markRead,
                    onDismiss = { sheetIndex = null }
                )
                loaded -> LaunchedEffect(Unit) { sheetIndex = null }
            }
        }
    }
}

/**
 * One tab of the Saved screen: a summary card and the rows, with the Quiz and Flashcards actions
 * pinned underneath. Both tabs use it, so they always look and behave the same.
 */
@Composable
private fun SavedPane(
    entries: List<SavedEntry>?,
    summaryIcon: ImageVector,
    summaryTitle: (Int) -> String,
    emptyIcon: ImageVector,
    emptyText: String,
    actionIcon: ImageVector,
    actionTint: Color,
    actionDescription: String,
    onAction: (Long) -> Unit,
    onOpen: (words: List<WordEntity>, index: Int) -> Unit,
    onQuiz: () -> Unit,
    onFlashcards: () -> Unit
) {
    when {
        entries == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        entries.isEmpty() -> EmptyState(icon = emptyIcon, text = emptyText)

        else -> Column(modifier = Modifier.fillMaxSize()) {
            val breakdown = remember(entries) {
                SavedSummary.typeCounts(entries.map { it.word })
                    .joinToString("  \u2022  ") { (type, count) -> type.countText(count) }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(Spacing.Content),
                verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
            ) {
                item(key = "summary") {
                    SummaryCard(
                        icon = summaryIcon,
                        title = summaryTitle(entries.size),
                        subtitle = breakdown
                    )
                }
                itemsIndexed(entries, key = { _, entry -> entry.word.id }) { index, entry ->
                    SavedWordRow(
                        entry = entry,
                        actionIcon = actionIcon,
                        actionTint = actionTint,
                        actionDescription = actionDescription,
                        onClick = { onOpen(entries.map { it.word }, index) },
                        onAction = { onAction(entry.word.id) }
                    )
                }
            }
            ActionBar(
                primaryLabel = "Quiz",
                primaryIcon = AppIcons.Pencil,
                onPrimary = onQuiz,
                secondaryLabel = "Flashcards",
                secondaryIcon = AppIcons.Cards,
                onSecondary = onFlashcards
            )
        }
    }
}

@Composable
private fun SavedWordRow(
    entry: SavedEntry,
    actionIcon: ImageVector,
    actionTint: Color,
    actionDescription: String,
    onClick: () -> Unit,
    onAction: () -> Unit
) {
    val word = entry.word
    // A badge with the type replaces the part of speech for idioms, proverbs and the like.
    val typeBadge = entry.missed == 0 && word.type != WordType.WORD

    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .heightIn(min = 68.dp)
                .padding(
                    start = Spacing.Content,
                    top = Spacing.Compact,
                    bottom = Spacing.Compact,
                    end = Spacing.Micro
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WordSummary(
                word = word,
                modifier = Modifier.weight(1f),
                showPartOfSpeech = !typeBadge
            )
            if (entry.missed > 0) {
                TagBadge(
                    text = "Missed ${entry.missed}x",
                    color = AppColors.danger,
                    modifier = Modifier.padding(start = Spacing.Compact)
                )
            } else if (typeBadge) {
                TagBadge(
                    text = word.type.shortLabel(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Spacing.Compact)
                )
            }
            IconButton(onClick = onAction) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionDescription,
                    tint = actionTint
                )
            }
        }
    }
}
