package com.vocabulary.app.ui.detail

import com.vocabulary.app.ui.components.exampleLines
import com.vocabulary.app.ui.components.ExampleBullets
import com.vocabulary.app.ui.theme.TimesNewRoman
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.theme.bn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.tts.TtsStatus
import com.vocabulary.app.ui.components.shortPartOfSpeech
import com.vocabulary.app.ui.groups.TermList
import com.vocabulary.app.ui.groups.antonymPairs
import com.vocabulary.app.ui.groups.synonymPairs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isBookmarked.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.ttsStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ttsStatus) {
        if (ttsStatus == TtsStatus.UNAVAILABLE) {
            snackbarHostState.showSnackbar(
                "Text-to-speech is not available. Install an English voice in your device settings."
            )
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Word details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Speaker and star sit together at the top right.
                    (uiState as? WordDetailUiState.Found)?.let { found ->
                        IconButton(onClick = { viewModel.speak(found.word.word) }) {
                            Icon(
                                AppIcons.Volume,
                                contentDescription = "Pronounce",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = viewModel::toggleBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) AppIcons.StarFilled else AppIcons.Star,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            WordDetailUiState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            WordDetailUiState.NotFound -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "This word could not be found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is WordDetailUiState.Found -> WordDetailContent(
                word = state.word,
                onSpeak = viewModel::speak,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordDetailContent(
    word: WordEntity,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header card
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = word.word,
                            fontFamily = TimesNewRoman,
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            text = word.partOfSpeech.shortPartOfSpeech(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }

        // Meaning
        Section(title = "Meaning") {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = word.banglaMeaning.bn(),
                        fontFamily = TimesNewRoman,
                        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 32.sp),
                    )
                    Text(
                        text = word.englishMeaning,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = TimesNewRoman,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // "Careful (যত্নশীল), precise (সুনির্দিষ্ট)": one running line, like the popup.
        if (word.synonyms.isNotEmpty()) {
            Section(title = "Synonyms") { TermList(word.synonymPairs()) }
        }
        if (word.antonyms.isNotEmpty()) {
            Section(title = "Antonyms") { TermList(word.antonymPairs()) }
        }

        if (word.example.isNotBlank()) {
            Section(title = "Example") {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExampleBullets(
                            example = word.example,
                            fontFamily = TimesNewRoman,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onSpeak(word.exampleLines().joinToString(" ")) }) {
                            Icon(
                                AppIcons.Volume,
                                contentDescription = "Read the example aloud"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        content()
    }
}

