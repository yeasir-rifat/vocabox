package com.vocabulary.app.ui.groups

import com.vocabulary.app.ui.components.ExampleBullets
import com.vocabulary.app.ui.theme.TimesNewRoman
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.domain.prepositionSplit
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.shortPartOfSpeech
import com.vocabulary.app.ui.theme.Spacing
import com.vocabulary.app.ui.theme.bn

/** Bangla meanings of one entry, joined for one line: "পরোপকারী; দয়ালু" becomes "পরোপকারী, দয়ালু". */
internal fun WordEntity.banglaMeaningLine(): String =
    banglaMeaning.split(';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(", ")

/** Idioms and proverbs are whole phrases: long titles, and "idiom" or "proverb" is not a part of speech. */
internal fun WordEntity.isPhrase(): Boolean = type == WordType.IDIOM || type == WordType.PROVERB

/** One-word substitution cards show the phrase and hide the word, so the card works as a recall prompt. */
internal fun WordEntity.isOneWord(): Boolean = type == WordType.ONE_WORD_SUB

/** Short part of speech for the title; empty for phrases. */
internal fun WordEntity.posLabel(): String =
    if (isPhrase() || type == WordType.PREPOSITION) "" else partOfSpeech.shortPartOfSpeech()

/** The example without quotes, with the phrase in bold when it appears in the sentence as written. */
internal fun highlightPhrase(example: String, phrase: String, color: androidx.compose.ui.graphics.Color): AnnotatedString {
    val target = phrase.trim()
    val start = if (target.isEmpty()) -1 else example.indexOf(target, ignoreCase = true)
    return buildAnnotatedString {
        if (start >= 0) {
            append(example.substring(0, start))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                append(example.substring(start, start + target.length))
            }
            append(example.substring(start + target.length))
        } else {
            append(example)
        }
    }
}

/** Each synonym with its Bangla meaning (empty when the data has none). */
internal fun WordEntity.synonymPairs(): List<Pair<String, String>> =
    synonyms.mapIndexed { i, term -> term to synonymsBn.getOrNull(i).orEmpty() }

/** Each antonym with its Bangla meaning (empty when the data has none). */
internal fun WordEntity.antonymPairs(): List<Pair<String, String>> =
    antonyms.mapIndexed { i, term -> term to antonymsBn.getOrNull(i).orEmpty() }

/**
 * Blurs and dims whatever is behind the dialog window. Real window blur needs Android 12+ and a
 * device that allows it; elsewhere the background is only dimmed, a little more strongly.
 */
@Composable
private fun BlurBehindEffect(blurRadius: androidx.compose.ui.unit.Dp = 24.dp) {
    val view = LocalView.current
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply {
                blurBehindRadius = with(density) { blurRadius.roundToPx() }
            }
            window.setDimAmount(0.3f)
        } else {
            window.setDimAmount(0.6f)
        }
    }
}

/**
 * Floating card with everything about one word: word and short part of speech, Bangla meaning,
 * English explanation, synonyms and antonyms (each with its Bangla meaning in brackets) and the
 * example. Sections are split by a light separator line. The header stays pinned while the body scrolls.
 */
@Composable
fun WordPopup(
    word: WordEntity,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    related: List<WordEntity> = emptyList(),
    onRelatedClick: (WordEntity) -> Unit = {}
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BlurBehindEffect()
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .padding(horizontal = Spacing.Content)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
        ) {
            Column {
                PopupHeader(
                    word = word,
                    isBookmarked = isBookmarked,
                    onToggleBookmark = onToggleBookmark,
                    onSpeak = onSpeak,
                    onClose = onDismiss
                )
                Separator(modifier = Modifier.padding(horizontal = Spacing.Card))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(androidx.compose.runtime.key(word.id) { rememberScrollState() })
                        .padding(horizontal = Spacing.Card, vertical = Spacing.Standard),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Standard)
                ) {
                    WordSections(
                        word = word,
                        related = related,
                        onRelatedClick = onRelatedClick
                    )
                }
            }
        }
    }
}

/** The sections of a word (meanings, synonyms, antonyms, example, related), shared by the popup and the Word List sheet. */
@Composable
internal fun WordSections(
    word: WordEntity,
    related: List<WordEntity> = emptyList(),
    onRelatedClick: (WordEntity) -> Unit = {}
) {
    val synonyms = word.synonymPairs()
    val antonyms = word.antonymPairs()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Standard)) {
        PopupSection(title = "Bengali Meaning") {
            Text(
                text = word.banglaMeaningLine().bn(),
                fontFamily = TimesNewRoman,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Separator()
        PopupSection(title = if (word.isOneWord()) "Phrase" else "English Meaning") {
            Text(
                text = word.englishMeaning,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = TimesNewRoman
            )
        }
        if (synonyms.isNotEmpty()) {
            Separator()
            PopupSection(title = "Synonyms") { TermList(synonyms) }
        }
        if (antonyms.isNotEmpty()) {
            Separator()
            PopupSection(title = "Antonyms") { TermList(antonyms) }
        }
        if (word.example.isNotBlank()) {
            Separator()
            PopupSection(title = "Examples") {
                val accent = MaterialTheme.colorScheme.primary
                ExampleBullets(
                    example = word.example,
                    fontFamily = TimesNewRoman,
                    render = { highlightPhrase(it, word.word, accent) }
                )
            }
        }
        if (related.isNotEmpty()) {
            Separator()
            PopupSection(title = "Related") {
                RelatedList(related, onRelatedClick)
            }
        }
    }
}

@Composable
internal fun PopupHeader(
    word: WordEntity,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit
) {
    val split = word.prepositionSplit()
    val title = buildAnnotatedString {
        if (split != null) {
            // The preposition that the card hid is revealed here, in the accent colour.
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(split.first + " ") }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                append(split.second)
            }
        } else {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(word.word) }
        }
        val pos = word.posLabel()
        if (pos.isNotEmpty()) {
            append("  ")
            withStyle(
                SpanStyle(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { append(pos) }
        }
    }
    if (word.isPhrase()) {
        // Long phrase: actions on their own row so the phrase can use the full width.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.Card, end = Spacing.Compact, top = Spacing.Compact, bottom = Spacing.Standard)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderActions(word, isBookmarked, onToggleBookmark, onSpeak, onClose)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = TimesNewRoman,
                modifier = Modifier.padding(end = Spacing.Compact)
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.Card, end = Spacing.Compact, top = Spacing.Compact, bottom = Spacing.Compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = TimesNewRoman,
                modifier = Modifier.weight(1f)
            )
            HeaderActions(word, isBookmarked, onToggleBookmark, onSpeak, onClose)
        }
    }
}

@Composable
private fun HeaderActions(
    word: WordEntity,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit
) {
    IconButton(onClick = { onSpeak(word.word) }) {
        Icon(
            AppIcons.Volume,
            contentDescription = "Pronounce",
            tint = MaterialTheme.colorScheme.primary
        )
    }
    IconButton(onClick = onToggleBookmark) {
        Icon(
            imageVector = if (isBookmarked) AppIcons.StarFilled else AppIcons.Star,
            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
            tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    IconButton(onClick = onClose) {
        Icon(AppIcons.Close, contentDescription = "Close")
    }
}

@Composable
private fun PopupSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Compact)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

/** "Forsake (পরিত্যাগ করা), desert (ত্যাগ করে যাওয়া), ...": one comma separated line, wrapping as needed. */
@Composable
private fun TermList(terms: List<Pair<String, String>>) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = buildAnnotatedString {
            terms.forEachIndexed { index, (rawTerm, bangla) ->
                if (index > 0) append(", ")
                val term = if (index == 0) rawTerm.replaceFirstChar { it.uppercase() } else rawTerm
                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(term) }
                if (bangla.isNotBlank()) {
                    withStyle(SpanStyle(color = muted)) {
                        append(" (")
                        append(bangla.bn())
                        append(")")
                    }
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = TimesNewRoman
    )
}

/** Other particles of the same verb. Tapping one opens its popup. */
@Composable
private fun RelatedList(items: List<WordEntity>, onClick: (WordEntity) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    Column {
        items.forEach { item ->
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = accent)) { append(item.word) }
                    val bangla = item.banglaMeaningLine()
                    if (bangla.isNotEmpty()) {
                        withStyle(SpanStyle(color = muted)) {
                            append(" (")
                            append(bangla.bn())
                            append(")")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = TimesNewRoman,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(item) }
                    .heightIn(min = Spacing.Target)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        }
    }
}

/** Light hairline between sections. */
@Composable
internal fun Separator(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}
