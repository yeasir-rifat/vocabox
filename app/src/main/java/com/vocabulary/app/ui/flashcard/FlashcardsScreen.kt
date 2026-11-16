package com.vocabulary.app.ui.flashcard

import com.vocabulary.app.ui.theme.TimesNewRoman
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.theme.bn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.tts.TtsStatus
import com.vocabulary.app.ui.components.DropdownFilterChip
import com.vocabulary.app.ui.components.label
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
    val topics by viewModel.topics.collectAsStateWithLifecycle()
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
                topics = topics,
                onSource = viewModel::onSourceSelected,
                onSize = viewModel::onSizeSelected,
                onType = viewModel::onTypeSelected,
                onTopic = viewModel::onTopicSelected,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupContent(
    config: DeckConfig,
    counts: DeckCounts,
    topics: List<String>,
    onSource: (DeckSource) -> Unit,
    onSize: (Int) -> Unit,
    onType: (WordType?) -> Unit,
    onTopic: (String?) -> Unit,
    onLevel: (Int?) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val available = counts.available(config.source)
    val sessionSize = minOf(config.size, available)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Summary
        AppCard(
            modifier = Modifier.fillMaxWidth(),
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
                when (config.source) {
                    DeckSource.SMART -> {
                        StatBlock(counts.due, "Due for review")
                        StatBlock(counts.fresh, "New words")
                    }
                    DeckSource.BOOKMARKED -> StatBlock(counts.bookmarked, "Bookmarked")
                    DeckSource.MISTAKES -> StatBlock(counts.mistakes, "Words to practice")
                }
            }
        }

        SectionTitle("Deck") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeckSource.entries.forEach { source ->
                    FilterChip(
                        selected = config.source == source,
                        onClick = { onSource(source) },
                        label = { Text(source.title()) }
                    )
                }
            }
        }

        if (config.source == DeckSource.SMART) {
            SectionTitle("Filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DropdownFilterChip(
                        title = "Type",
                        selected = config.type,
                        options = WordType.entries.toList(),
                        optionLabel = { it.label() },
                        onSelected = onType
                    )
                    DropdownFilterChip(
                        title = "Topic",
                        selected = config.topic,
                        options = topics,
                        optionLabel = { it },
                        onSelected = onTopic
                    )
                    DropdownFilterChip(
                        title = "Level",
                        selected = config.level,
                        options = listOf(1, 2, 3),
                        optionLabel = { levelLabel(it) },
                        onSelected = onLevel
                    )
                }
            }
        }

        SectionTitle("Cards per session") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeckConfig.SIZES.forEach { size ->
                    FilterChip(
                        selected = config.size == size,
                        onClick = { onSize(size) },
                        label = { Text("$size") }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                shape = MaterialTheme.shapes.small,
                onClick = onStart,
                enabled = available > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (available > 0) "Start ($sessionSize cards)" else "No cards available")
            }
            if (available == 0) {
                Text(
                    text = when (config.source) {
                        DeckSource.SMART -> "Nothing is due and no new words match these filters."
                        DeckSource.BOOKMARKED -> "Tap the star on a word to study it here."
                        DeckSource.MISTAKES -> "Words you miss in quizzes will appear here."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
