package com.vocabulary.app.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.ui.groups.PopupHeader
import com.vocabulary.app.ui.groups.Separator
import com.vocabulary.app.ui.groups.WordSections
import com.vocabulary.app.ui.theme.Spacing

/**
 * Bottom sheet of the word screens. It opens half way and grows to full height when dragged up.
 * It is a real modal sheet, so the scrim covers the navigation bar, Back closes it, and the system
 * bar insets are handled. Swiping left or right moves to the next or previous word; the counter in
 * the handle shows where you are.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSheet(
    words: List<WordEntity>,
    startIndex: Int,
    bookmarkedIds: Set<Long>,
    onToggleBookmark: (Long) -> Unit,
    onSpeak: (String) -> Unit,
    onWordShown: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (words.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, words.lastIndex)) { words.size }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val currentWords by rememberUpdatedState(words)
    val currentOnShown by rememberUpdatedState(onWordShown)

    // Showing a word counts as reading it.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentWords.getOrNull(page)?.let { currentOnShown(it.id) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    text = "${pagerState.currentPage + 1}/${words.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.Micro)
                )
            }
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            key = { words[it].id }
        ) { page ->
            val word = words[page]
            Column(modifier = Modifier.fillMaxHeight()) {
                PopupHeader(
                    word = word,
                    isBookmarked = word.id in bookmarkedIds,
                    onToggleBookmark = { onToggleBookmark(word.id) },
                    onSpeak = onSpeak,
                    onClose = onDismiss
                )
                Separator(modifier = Modifier.padding(horizontal = Spacing.Card))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.Card, vertical = Spacing.Standard)
                ) {
                    WordSections(word = word)
                }
            }
        }
    }
}
