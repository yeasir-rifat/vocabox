package com.vocabulary.app.ui.list

import com.vocabulary.app.ui.components.AppIcons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.clickable
import com.vocabulary.app.ui.components.EmptyState
import com.vocabulary.app.ui.components.WordSummary
import com.vocabulary.app.ui.theme.bn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.vocabulary.app.data.model.WordOrder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.ui.components.DropdownFilterChip
import com.vocabulary.app.ui.components.shortPartOfSpeech
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vocabulary.app.ui.components.label
import kotlinx.coroutines.launch

@Composable
fun WordsScreen(
    onBack: () -> Unit,
    onWordClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordsViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val words by viewModel.words.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()
    val availableLetters by viewModel.availableLetters.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    // Position (in the shown list) of the word open in the bottom sheet; null when it is closed.
    var sheetIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val openWord: (Long) -> Unit = { id ->
        val index = words?.indexOfFirst { it.id == id } ?: -1
        if (index >= 0) sheetIndex = index
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(AppIcons.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Word List",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            IconButton(onClick = { settingsOpen = true }) {
                Icon(AppIcons.Settings, contentDescription = "List settings")
            }
        }

        // Search
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search a word or Bangla meaning", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(AppIcons.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        // What the list is showing right now
        ListSummaryRow(
            options = options,
            filters = filters,
            resultCount = words?.size ?: 0,
            onClick = { settingsOpen = true },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Content
        val currentWords = words
        Box(modifier = Modifier.weight(1f)) {
        when {
                currentWords == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                currentWords.isEmpty() -> EmptyState(
                    icon = AppIcons.SearchOff,
                    text = if (options.isActive || filters.isActive) "No words match these settings."
                    else "No words match your search."
                )

                // Letter sections and the side index only make sense in A-Z order.
                options.order == WordOrder.A_TO_Z -> WordListWithIndex(
                    words = currentWords,
                    showStrip = options.showStrip,
                    bookmarkedIds = bookmarkedIds,
                    onWordClick = openWord,
                    onToggleBookmark = viewModel::toggleBookmark,
                    modifier = Modifier
                )

                else -> PlainWordList(
                    words = currentWords,
                    bookmarkedIds = bookmarkedIds,
                    onWordClick = openWord,
                    onToggleBookmark = viewModel::toggleBookmark,
                    modifier = Modifier
                )
        }
        }
    }

    val sheetWords = words
    val startIndex = sheetIndex
    if (startIndex != null && !sheetWords.isNullOrEmpty()) {
        WordSheet(
            words = sheetWords,
            startIndex = startIndex,
            bookmarkedIds = bookmarkedIds,
            onToggleBookmark = viewModel::toggleBookmark,
            onSpeak = viewModel::speak,
            onWordShown = viewModel::markRead,
            onDismiss = { sheetIndex = null }
        )
    }

    ListOptionsSidebar(
        visible = settingsOpen,
        options = options,
        filters = filters,
        resultCount = words?.size ?: 0,
        onOptionsChange = viewModel::setOptions,
        availableLetters = availableLetters,
        onToggleLetter = viewModel::toggleLetter,
        onClearLetters = viewModel::clearLetters,
        stripEnabled = options.showStrip,
        onStripChange = { viewModel.setOptions(options.copy(showStrip = it)) },
        onTypeSelected = viewModel::onTypeSelected,
        onShuffleAgain = viewModel::shuffleAgain,
        onReset = viewModel::resetOptions,
        onClose = { settingsOpen = false }
    )
    }
}

@Composable
private fun PlainWordList(
    words: List<WordEntity>,
    bookmarkedIds: Set<Long>,
    onWordClick: (Long) -> Unit,
    onToggleBookmark: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(words, key = { it.id }) { word ->
            WordRow(
                word = word,
                isBookmarked = word.id in bookmarkedIds,
                onClick = { onWordClick(word.id) },
                onToggleBookmark = { onToggleBookmark(word.id) }
            )
        }
    }
}

private fun WordEntity.indexLetter(): Char {
    val c = word.firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordListWithIndex(
    words: List<WordEntity>,
    showStrip: Boolean,
    bookmarkedIds: Set<Long>,
    onWordClick: (Long) -> Unit,
    onToggleBookmark: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Group by first letter; '#' (non-letters) goes last.
    val groups = remember(words) {
        words.groupBy { it.indexLetter() }
            .toList()
            .sortedBy { (letter, _) -> if (letter == '#') '{' else letter }
    }

    // First LazyColumn index of every letter section (its items; the list has no letter headers).
    val startIndexes = remember(groups) {
        val map = LinkedHashMap<Char, Int>()
        var index = 0
        groups.forEach { (letter, list) ->
            map[letter] = index
            index += list.size
        }
        map
    }

    val indexLetters = remember(startIndexes) {
        ('A'..'Z').toList() + if ('#' in startIndexes) listOf('#') else emptyList()
    }

    val currentLetter by remember(startIndexes) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            startIndexes.entries.lastOrNull { it.value <= first }?.key
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = if (showStrip) 44.dp else 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            groups.forEach { (_, list) ->
                items(list, key = { it.id }) { word ->
                    WordRow(
                        word = word,
                        isBookmarked = word.id in bookmarkedIds,
                        onClick = { onWordClick(word.id) },
                        onToggleBookmark = { onToggleBookmark(word.id) }
                    )
                }
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
                    target?.let { scope.launch { listState.scrollToItem(startIndexes.getValue(it)) } }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun WordRow(
    word: WordEntity,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 76.dp)
                .padding(start = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WordSummary(word = word, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) AppIcons.StarFilled else AppIcons.Star,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
