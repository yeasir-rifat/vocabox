package com.vocabulary.app.ui.list

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.ui.groups.PopupHeader
import com.vocabulary.app.ui.groups.Separator
import com.vocabulary.app.ui.groups.WordSections
import com.vocabulary.app.ui.theme.Spacing

/** The sheet first opens to this share of the screen height. */
private const val COLLAPSED_FRACTION = 0.62f

/**
 * Bottom sheet of the word screens. It opens to 62% of the screen and grows when dragged up (or when
 * the content is scrolled up), but never covers the whole screen: a gap stays above it. Dragging down
 * shrinks it back, and once more closes it. It is a real modal sheet, so the scrim covers the
 * navigation bar, Back closes it, and the system bar insets are handled. Swiping left or right moves
 * to the next or previous word; the counter in the handle shows where you are.
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
    // One resting position only (the height is driven below), so Material's own 50% stop is skipped.
    // Material's own drag gesture is vetoed (the installed material3 has no sheetGesturesEnabled
    // parameter): the sheet may only go to Hidden when close() asks for it. Our own handle/content
    // drag logic below drives the height instead.
    var allowHide by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowHide }
    )
    val scope = rememberCoroutineScope()
    val currentWords by rememberUpdatedState(words)
    val currentOnShown by rememberUpdatedState(onWordShown)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // Showing a word counts as reading it.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentWords.getOrNull(page)?.let { currentOnShown(it.id) }
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val topGap = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
    val collapsedHeight = screenHeight * COLLAPSED_FRACTION
    val expandedHeight = maxOf(collapsedHeight, screenHeight - topGap)

    var expanded by remember { mutableStateOf(false) }
    val sheetHeight by animateDpAsState(
        targetValue = if (expanded) expandedHeight else collapsedHeight,
        label = "wordSheetHeight"
    )

    fun close() {
        allowHide = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { currentOnDismiss() }
    }

    // Drag up: grow. Drag down: shrink, then close.
    val dragThreshold = with(LocalDensity.current) { 48.dp.toPx() }
    var handleDrag by remember { mutableFloatStateOf(0f) }
    val handleDragState = rememberDraggableState { handleDrag += it }
    val contentConnection = remember(dragThreshold) {
        object : NestedScrollConnection {
            private var pulled = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f) {
                    pulled = 0f
                    if (!expanded) expanded = true
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // The list is already at its top and the finger keeps moving down.
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > dragThreshold) {
                        pulled = 0f
                        if (expanded) expanded = false else close()
                    }
                }
                return Offset.Zero
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = handleDragState,
                        orientation = Orientation.Vertical,
                        onDragStarted = { handleDrag = 0f },
                        onDragStopped = {
                            when {
                                handleDrag < -dragThreshold -> expanded = true
                                handleDrag > dragThreshold -> if (expanded) expanded = false else close()
                            }
                            handleDrag = 0f
                        }
                    )
            ) {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { words[it].id }
            ) { page ->
                val word = words[page]
                Column(modifier = Modifier.fillMaxHeight()) {
                    PopupHeader(
                        word = word,
                        isBookmarked = word.id in bookmarkedIds,
                        onToggleBookmark = { onToggleBookmark(word.id) },
                        onSpeak = onSpeak,
                        onClose = ::close
                    )
                    Separator(modifier = Modifier.padding(horizontal = Spacing.Card))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(contentConnection)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.Card, vertical = Spacing.Standard)
                    ) {
                        WordSections(word = word)
                    }
                }
            }
        }
    }
}
