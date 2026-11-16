package com.vocabulary.app.ui.flashcard

import com.vocabulary.app.ui.groups.banglaMeaningLine
import com.vocabulary.app.ui.groups.antonymPairs
import com.vocabulary.app.ui.groups.synonymPairs
import com.vocabulary.app.ui.groups.posLabel
import com.vocabulary.app.ui.groups.highlightPhrase
import com.vocabulary.app.ui.components.ExampleBullets
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.background
import com.vocabulary.app.ui.theme.TimesNewRoman
import com.vocabulary.app.ui.components.AppProgressBar
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.components.AppCard
import com.vocabulary.app.ui.theme.AppColors
import com.vocabulary.app.ui.theme.bn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.ui.components.LevelBadge
import com.vocabulary.app.ui.components.shortPartOfSpeech
import com.vocabulary.app.ui.components.singularLabel
import kotlinx.coroutines.launch


/** Share of the screen width a card must travel before a release counts as an answer. */
private const val SWIPE_THRESHOLD_FRACTION = 0.28f

@Composable
internal fun StudyContent(
    state: SessionState.Active,
    onAnswer: (Boolean) -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProgressHeader(resolved = state.resolved, total = state.total)
        // A new key after every answer gives the next card a fresh position and flip state.
        key(state.serial) {
            CardWithControls(
                state = state,
                onAnswer = onAnswer,
                onSpeak = onSpeak,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProgressHeader(resolved: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$resolved of $total done",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AppProgressBar(
            progress = if (total == 0) 0f else resolved / total.toFloat(),
            label = "studyProgress"
        )
    }
}

@Composable
private fun CardWithControls(
    state: SessionState.Active,
    onAnswer: (Boolean) -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var flipped by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(false) }

    val widthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val threshold = widthPx * SWIPE_THRESHOLD_FRACTION

    // Throws the card off screen, then reports the answer. Ignored after the first call.
    val fling: (Boolean) -> Unit = { known ->
        if (!answered) {
            answered = true
            scope.launch {
                offsetX.animateTo(
                    targetValue = if (known) widthPx * 1.4f else -widthPx * 1.4f,
                    animationSpec = tween(durationMillis = 200)
                )
                onAnswer(known)
            }
        }
    }

    val swipeStrength = (offsetX.value / threshold).coerceIn(-1f, 1f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Card waiting underneath.
            if (state.hasNext) {
                AppCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 0.94f
                            scaleY = 0.94f
                            translationY = 28f
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {}
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = offsetX.value / widthPx * 14f
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val x = offsetX.value
                                when {
                                    x > threshold -> fling(true)
                                    x < -threshold -> fling(false)
                                    else -> scope.launch { offsetX.animateTo(0f, spring()) }
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, spring()) }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                if (!answered) {
                                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                                }
                            }
                        )
                    }
            ) {
                FlipCard(
                    word = state.current,
                    flipped = flipped,
                    onFlip = { flipped = !flipped },
                    onSpeak = onSpeak
                )
                if (swipeStrength > 0f) {
                    SwipeLabel(
                        text = "KNOW",
                        color = AppColors.success,
                        strength = swipeStrength,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                } else if (swipeStrength < 0f) {
                    SwipeLabel(
                        text = "DON'T KNOW",
                        color = AppColors.danger,
                        strength = -swipeStrength,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }

        Text(
            text = "Tap to flip. Swipe right if you know it, left if you don't.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                shape = MaterialTheme.shapes.small,
                onClick = { fling(false) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(AppIcons.Close, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Don't know")
            }
            Button(
                shape = MaterialTheme.shapes.small,
                onClick = { fling(true) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(AppIcons.Check, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("I know it")
            }
        }
    }
}

@Composable
private fun SwipeLabel(
    text: String,
    color: Color,
    strength: Float,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
            .padding(16.dp)
            .graphicsLayer { alpha = strength }
            .border(2.dp, color, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/**
 * A word card. Tap to flip. The star (when [onToggleBookmark] is given) and the speaker sit together in
 * the top right corner of both faces.
 */
@Composable
internal fun FlipCard(
    word: WordEntity,
    flipped: Boolean,
    onFlip: () -> Unit,
    onSpeak: (String) -> Unit,
    isBookmarked: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "flip"
    )
    val density = LocalDensity.current.density

    Card(
        onClick = onFlip,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            },
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (rotation <= 90f) {
            CardFront(word, onSpeak, isBookmarked, onToggleBookmark)
        } else {
            // The back face is mirrored by the parent rotation, so rotate it back.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                CardBack(word, onSpeak, isBookmarked, onToggleBookmark)
            }
        }
    }
}

/** Star and speaker, side by side. */
@Composable
private fun CardActions(
    word: WordEntity,
    onSpeak: (String) -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onToggleBookmark != null) {
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) AppIcons.StarFilled else AppIcons.Star,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = { onSpeak(word.word) }) {
            Icon(
                AppIcons.Volume,
                contentDescription = "Pronounce",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Font size of a headword: the longer the phrase, the smaller. */
private fun frontFontSize(text: String) = when {
    text.length <= 14 -> 40.sp
    text.length <= 28 -> 32.sp
    else -> 26.sp
}

/** Font size of the Bangla meaning on the back: long sentences get a smaller size. */
private fun meaningFontSize(text: String) = when {
    text.length <= 18 -> 26.sp
    text.length <= 30 -> 22.sp
    else -> 19.sp
}

@Composable
private fun CardFront(
    word: WordEntity,
    onSpeak: (String) -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: (() -> Unit)?
) {
    val typeLabel = word.type.singularLabel()
    val pos = word.partOfSpeech.shortPartOfSpeech()
    // The part of speech of an idiom is "idiom" again, so it is shown only when it adds something.
    val showPos = pos.isNotBlank() && !pos.trim('.').equals(typeLabel, ignoreCase = true)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = typeLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = word.word,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = frontFontSize(word.word),
                    lineHeight = frontFontSize(word.word) * 1.2f
                ),
                fontFamily = TimesNewRoman,
                textAlign = TextAlign.Center
            )
            if (showPos) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pos,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = TimesNewRoman,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
        ) {
            CardActions(word, onSpeak, isBookmarked, onToggleBookmark)
        }
    }
}

@Composable
private fun CardBack(
    word: WordEntity,
    onSpeak: (String) -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: (() -> Unit)?
) {
    val synonyms = word.synonymPairs()
    val antonyms = word.antonymPairs()
    val pos = word.posLabel()
    val bangla = word.banglaMeaningLine()
    Column(modifier = Modifier.fillMaxSize()) {
        // Header: the word with its part of speech, and the star and speaker in the corner.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append(word.word)
                    }
                    if (pos.isNotEmpty()) {
                        append("  ")
                        withStyle(
                            SpanStyle(
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { append(pos) }
                    }
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, lineHeight = 40.sp),
                fontFamily = TimesNewRoman,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            CardActions(word, onSpeak, isBookmarked, onToggleBookmark)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meaning block: Bangla first, English under it.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = bangla.bn(),
                    fontFamily = TimesNewRoman,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = meaningFontSize(bangla),
                        lineHeight = meaningFontSize(bangla) * 1.45f
                    )
                )
                Text(
                    text = word.englishMeaning,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = TimesNewRoman,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (synonyms.isNotEmpty()) {
                BackSection("Synonyms") { TermLine(synonyms) }
            }
            if (antonyms.isNotEmpty()) {
                BackSection("Antonyms") { TermLine(antonyms) }
            }
            if (word.example.isNotBlank()) {
                val accent = MaterialTheme.colorScheme.primary
                BackSection("Examples") {
                    ExampleBullets(
                        example = word.example,
                        fontFamily = TimesNewRoman,
                        render = { highlightPhrase(it, word.word, accent) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

/** "Forsake (পরিত্যাগ করা), desert (ত্যাগ করে যাওয়া)": one comma separated line. */
@Composable
private fun TermLine(terms: List<Pair<String, String>>) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = buildAnnotatedString {
            terms.forEachIndexed { index, (rawTerm, bn) ->
                if (index > 0) append(", ")
                val term = if (index == 0) rawTerm.replaceFirstChar { it.uppercase() } else rawTerm
                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(term) }
                if (bn.isNotBlank()) {
                    withStyle(SpanStyle(color = muted)) {
                        append(" (")
                        append(bn.bn())
                        append(")")
                    }
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = TimesNewRoman
    )
}
