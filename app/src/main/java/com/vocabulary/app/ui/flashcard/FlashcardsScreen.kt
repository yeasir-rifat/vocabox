package com.vocabulary.app.ui.flashcard

import com.vocabulary.app.ui.theme.TimesNewRoman
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.theme.bn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.DeckSource
import com.vocabulary.app.tts.TtsStatus
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.vocabulary.app.ui.components.SegmentedControl
import com.vocabulary.app.ui.components.levelLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    onBack: () -> Unit,
    onWordClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlashcardsViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.ttsStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ttsStatus) {
        if (ttsStatus == TtsStatus.UNAVAILABLE) {
            snackbarHostState.showSnackbar(
                "Text-to-speech is not available. Install an English voice in your device settings."
            )
        }
    }

    val active = session as? SessionState.Active

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Practice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (active != null) {
                        val isBookmarked = active.current.id in bookmarkedIds
                        IconButton(onClick = { viewModel.toggleBookmark(active.current.id) }) {
                            Icon(
                                imageVector = if (isBookmarked) AppIcons.StarFilled else AppIcons.Star,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (val state = session) {
            SessionState.Setup -> SetupContent(
                config = config,
                counts = counts,
                onSource = viewModel::onSourceSelected,
                onSize = viewModel::onSizeSelected,
                onLevel = viewModel::onLevelSelected,
                onStart = viewModel::start,
                modifier = contentModifier
            )

            SessionState.Loading -> Box(
                modifier = contentModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            SessionState.Empty -> EmptyContent(
                onBackToSetup = viewModel::backToSetup,
                modifier = contentModifier
            )

            is SessionState.Active -> StudyContent(
                state = state,
                onAnswer = viewModel::onAnswer,
                onSpeak = viewModel::speak,
                modifier = contentModifier
            )

            is SessionState.Finished -> SummaryContent(
                state = state,
                onWordClick = onWordClick,
                onStudyAgain = viewModel::backToSetup,
                onDone = onBack,
                modifier = contentModifier
            )
        }
    }
}

private fun DeckSource.title(): String = when (this) {
    DeckSource.SMART -> "Smart review"
    DeckSource.BOOKMARKED -> "Bookmarked"
    DeckSource.MISTAKES -> "Mistakes"
}

@Composable
private fun SetupContent(
    config: DeckConfig,
    counts: DeckCounts,
    onSource: (DeckSource) -> Unit,
    onSize: (Int) -> Unit,
    onLevel: (Int?) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val available = counts.available(config.source)
    val sessionSize = minOf(config.size, available)

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            ReadyCard(config.source, counts, available)

            PracticeSection("Deck") {
                DeckList(
                    counts = counts,
                    selected = config.source,
                    onSelect = onSource
                )
            }

            if (config.source == DeckSource.SMART) {
                PracticeSection("Level") {
                    SegmentedControl(
                        options = listOf<Int?>(null, 1, 2, 3),
                        selected = config.level,
                        label = { if (it == null) "All" else levelLabel(it) },
                        onSelect = onLevel
                    )
                }
            }

            PracticeSection("Cards per session") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentedControl(
                        options = DeckConfig.SIZES,
                        selected = config.size,
                        label = { "$it" },
                        onSelect = onSize
                    )
                    if (available in 1 until config.size) {
                        Text(
                            text = "Only $available cards available right now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Start button stays at the bottom, always reachable.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (available == 0) {
                    Text(
                        text = when (config.source) {
                            DeckSource.SMART -> "Nothing is due and no new words match this level."
                            DeckSource.BOOKMARKED -> "Tap the star on a word to study it here."
                            DeckSource.MISTAKES -> "Words you miss in quizzes will appear here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    shape = MaterialTheme.shapes.small,
                    onClick = onStart,
                    enabled = available > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (available > 0) "Start  ·  $sessionSize cards" else "No cards available",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/** Top card: one big number for what is ready now; for Smart review it is split into due and new. */
@Composable
private fun ReadyCard(source: DeckSource, counts: DeckCounts, available: Int) {
    val onCard = MaterialTheme.colorScheme.onPrimaryContainer
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = onCard
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(
                text = when (source) {
                    DeckSource.SMART -> "Ready to study"
                    DeckSource.BOOKMARKED -> "Bookmarked"
                    DeckSource.MISTAKES -> "To practice"
                },
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "$available",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "  cards",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (source == DeckSource.SMART) {
                HorizontalDivider(
                    color = onCard.copy(alpha = 0.14f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatColumn(counts.due, "Due for review", Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(onCard.copy(alpha = 0.14f))
                    )
                    StatColumn(counts.fresh, "New words", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

/** The three decks in one grouped card: name, short line, count and a radio mark per row. */
@Composable
private fun DeckList(
    counts: DeckCounts,
    selected: DeckSource,
    onSelect: (DeckSource) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        DeckSource.entries.forEachIndexed { index, source ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 70.dp)
                )
            }
            DeckRow(
                source = source,
                count = counts.available(source),
                selected = selected == source,
                onClick = { onSelect(source) }
            )
        }
    }
}

@Composable
private fun DeckRow(
    source: DeckSource,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val icon = when (source) {
        DeckSource.SMART -> AppIcons.Shuffle
        DeckSource.BOOKMARKED -> AppIcons.Star
        DeckSource.MISTAKES -> AppIcons.AlertCircle
    }
    val subtitle = when (source) {
        DeckSource.SMART -> "Due first, then new"
        DeckSource.BOOKMARKED -> "Words you starred"
        DeckSource.MISTAKES -> "Missed in quizzes"
    }
    val tile by animateColorAsState(
        if (selected) scheme.primary else scheme.surfaceVariant,
        tween(160), label = "deckTile"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "${source.title()}, $count cards" }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(tile),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.title(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) scheme.primary else Color.Transparent)
                .border(1.5.dp, if (selected) scheme.primary else scheme.onSurfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    AppIcons.Check,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** Small-caps section label above its content. */
@Composable
private fun PracticeSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}


@Composable
private fun StatBlock(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.displaySmall
        )
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SectionTitle(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun EmptyContent(onBackToSetup: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No cards to study",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Try a different deck or different filters.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        Button(shape = MaterialTheme.shapes.small, onClick = onBackToSetup) { Text("Back") }
    }
}

@Composable
private fun SummaryContent(
    state: SessionState.Finished,
    onWordClick: (Long) -> Unit,
    onStudyAgain: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accuracy = if (state.total == 0) 0 else state.knownFirstTry * 100 / state.total

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(64.dp)
                    )
                    Text(
                        text = "Session complete",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = if (state.missed.isEmpty()) {
                            "You knew every card."
                        } else {
                            "Missed cards will come back for review tomorrow."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                AppCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBlock(state.total, "Cards")
                        StatBlock(state.knownFirstTry, "Known first try")
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$accuracy%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Accuracy", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            if (state.missed.isNotEmpty()) {
                item {
                    Text(
                        text = "NEEDS MORE PRACTICE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.missed, key = { it.id }) { word ->
                    MissedRow(word = word, onClick = { onWordClick(word.id) })
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                onClick = onStudyAgain,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) { Text("Study again") }
            Button(
                shape = MaterialTheme.shapes.small,
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) { Text("Done") }
        }
    }
}

@Composable
private fun MissedRow(word: WordEntity, onClick: () -> Unit) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = word.word, style = MaterialTheme.typography.titleMedium, fontFamily = TimesNewRoman)
            Text(
                text = word.banglaMeaning.bn(),
                fontFamily = TimesNewRoman,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
