package com.vocabulary.app.ui.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.ui.groups.BlurBehindEffect
import com.vocabulary.app.ui.groups.PopupHeader
import com.vocabulary.app.ui.groups.Separator
import com.vocabulary.app.ui.groups.WordSections
import com.vocabulary.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/** The sheet always takes this share of the screen height. */
private const val SHEET_FRACTION = 0.70f

/**
 * Bottom sheet of the word screens. It takes a fixed 70% of the screen height and cannot be pulled
 * higher. Whatever is above it is dimmed and blurred (real blur on Android 12+, dim only elsewhere).
 * It closes with the X button, Back, a tap on the blurred area above, or by dragging the handle
 * (or the content, when it is already at its top) down. Swiping left or right moves to the next or
 * previous word; the counter in the handle shows where you are.
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

    // 1f = fully below the screen, 0f = fully shown.
    val hidden = remember { Animatable(1f) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hidden.animateTo(0f, tween(250)) }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            hidden.animateTo(1f, tween(200))
            currentOnDismiss()
        }
    }

    val dragThreshold = with(LocalDensity.current) { 120.dp.toPx() }
    val contentConnection = remember(dragThreshold) {
        object : NestedScrollConnection {
            private var pulled = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // The content is already at its top and the finger keeps moving down.
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > dragThreshold) {
                        pulled = 0f
                        close()
                    }
                } else if (available.y <= 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }
        }
    }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BlurBehindEffect()
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::close
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetHeightPx = constraints.maxHeight * SHEET_FRACTION
            val sheetHeight = with(LocalDensity.current) { sheetHeightPx.toDp() }
            val handleDragState = rememberDraggableState { delta ->
                // Down only: the sheet can never be pulled above its 70% position.
                scope.launch { hidden.snapTo((hidden.value + delta / sheetHeightPx).coerceIn(0f, 1f)) }
            }
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .graphicsLayer { translationY = size.height * hidden.value }
                    // Taps on the sheet itself must not reach the closing area behind it.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .draggable(
                                state = handleDragState,
                                orientation = Orientation.Vertical,
                                onDragStopped = { velocity ->
                                    if (hidden.value > 0.25f || velocity > 1500f) close()
                                    else hidden.animateTo(0f, tween(150))
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
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                                    .padding(
                                        start = Spacing.Card,
                                        end = Spacing.Card,
                                        top = Spacing.Standard,
                                        bottom = Spacing.Standard + navBottom
                                    )
                            ) {
                                WordSections(word = word)
                            }
                        }
                    }
                }
            }
        }
    }
}
