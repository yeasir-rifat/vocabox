package com.vocabulary.app.ui.flashcard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordOrder
import com.vocabulary.app.domain.initial
import com.vocabulary.app.tts.TtsStatus
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.EmptyState
import com.vocabulary.app.ui.list.AlphabetIndex
import com.vocabulary.app.ui.list.ListOptionsSidebar
import com.vocabulary.app.ui.list.ListSummaryRow
import kotlinx.coroutines.launch

/**
 * Flashcard page of the Words tab: the words of the shared list settings as cards. Tap a card to
 * flip it, swipe to move to the next or the previous word. The A-Z strip is off by default here
 * because the card needs the room.
 */
@Composable
fun WordCardsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordCardsViewModel = hiltViewModel()
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val words by viewModel.words.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()
    val availableLetters by viewModel.availableLetters.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.ttsStatus.collectAsStateWithLifecycle()
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ttsStatus) {
        if (ttsStatus == TtsStatus.UNAVAILABLE) {
            snackbarHostState.showSnackbar(
                "Text-to-speech is not available. Install an English voice in your device settings."
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Flashcards",
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
                resultCount = words?.size ?: 0,
                onClick = { settingsOpen = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val currentWords = words
            Box(modifier = Modifier.weight(1f)) {
                when {
                    currentWords == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    currentWords.isEmpty() -> EmptyState(
                        icon = AppIcons.SearchOff,
                        text = "No words match these settings."
                    )

                    else -> CardsPager(
                        words = currentWords,
                        bookmarkedIds = bookmarkedIds,
                        showStrip = options.cardsStrip && options.order == WordOrder.A_TO_Z,
                        onToggleBookmark = viewModel::toggleBookmark,
                        onSpeak = viewModel::speak,
                        onWordShown = viewModel::markRead
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        ListOptionsSidebar(
            visible = settingsOpen,
            options = options,
            filters = filters,
            resultCount = words?.size ?: 0,
            availableLetters = availableLetters,
            onToggleLetter = viewModel::toggleLetter,
            onClearLetters = viewModel::clearLetters,
            stripEnabled = options.cardsStrip,
            onStripChange = { viewModel.setOptions(options.copy(cardsStrip = it)) },
            onOptionsChange = viewModel::setOptions,
            onTypeSelected = viewModel::onTypeSelected,
            onLevelSelected = viewModel::onLevelSelected,
            onShuffleAgain = viewModel::shuffleAgain,
            onReset = viewModel::resetOptions,
            onClose = { settingsOpen = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardsPager(
    words: List<WordEntity>,
    bookmarkedIds: Set<Long>,
    showStrip: Boolean,
    onToggleBookmark: (Long) -> Unit,
    onSpeak: (String) -> Unit,
    onWordShown: (Long) -> Unit
) {
    val pagerState = rememberPagerState { words.size }
    val scope = rememberCoroutineScope()
    val currentWords by rememberUpdatedState(words)
    val currentOnShown by rememberUpdatedState(onWordShown)

    // A new set of words (settings changed) starts again from the first card.
    val signature = remember(words) { words.map { it.id } }
    LaunchedEffect(signature) { pagerState.scrollToPage(0) }

    // A card that is shown counts as read.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentWords.getOrNull(page)?.let { currentOnShown(it.id) }
        }
    }

    // First card of every letter, for the strip. Only meaningful in A-Z order.
    val startIndexes = remember(words) {
        val map = LinkedHashMap<Char, Int>()
        words.forEachIndexed { index, word -> map.putIfAbsent(word.initial(), index) }
        map
    }
    val indexLetters = remember(startIndexes) {
        ('A'..'Z').toList() + if ('#' in startIndexes) listOf('#') else emptyList()
    }
    val currentWord = words.getOrNull(pagerState.currentPage)
    val currentLetter = currentWord?.initial()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${words.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            if (currentWord != null) {
                val isBookmarked = currentWord.id in bookmarkedIds
                IconButton(
                    onClick = { onToggleBookmark(currentWord.id) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) AppIcons.StarFilled else AppIcons.Star,
                        contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                key = { words[it].id },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (showStrip) 36.dp else 0.dp)
            ) { page ->
                val word = words[page]
                var flipped by remember(word.id) { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                ) {
                    FlipCard(
                        word = word,
                        flipped = flipped,
                        onFlip = { flipped = !flipped },
                        onSpeak = onSpeak
                    )
                }
            }

            if (showStrip) {
                AlphabetIndex(
                    letters = indexLetters,
                    enabledLetters = startIndexes.keys,
                    currentLetter = currentLetter,
                    onLetterSelected = { letter ->
                        // Jump to the letter, or to the next letter that has words.
                        val target = indexLetters.firstOrNull { it >= letter && it in startIndexes }
                            ?: startIndexes.keys.lastOrNull()
                        target?.let { scope.launch { pagerState.scrollToPage(startIndexes.getValue(it)) } }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        }
    }
}
