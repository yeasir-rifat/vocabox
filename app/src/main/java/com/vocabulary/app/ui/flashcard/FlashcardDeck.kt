package com.vocabulary.app.ui.flashcard

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

@Composable
internal fun FlipCard(
    word: WordEntity,
    flipped: Boolean,
    onFlip: () -> Unit,
    onSpeak: (String) -> Unit
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
            CardFront(word, onSpeak)
        } else {
            // The back face is mirrored by the parent rotation, so rotate it back.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                CardBack(word, onSpeak)
            }
        }
    }
}

@Composable
private fun CardFront(word: WordEntity, onSpeak: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = word.type.singularLabel().uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = word.word,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 48.sp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = word.partOfSpeech.shortPartOfSpeech(),
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalIconButton(
            onClick = { onSpeak(word.word) },
            modifier = Modifier.size(52.dp)
        ) {
            Icon(AppIcons.Volume, contentDescription = "Pronounce")
        }
        Spacer(Modifier.height(16.dp))
        LevelBadge(word.level)
    }
}

@Composable
private fun CardBack(word: WordEntity, onSpeak: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onSpeak(word.word) }) {
                Icon(AppIcons.Volume, contentDescription = "Pronounce")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = word.banglaMeaning.bn(),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 38.sp)
            )
            Text(
                text = word.englishMeaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (word.synonyms.isNotEmpty()) {
            LabeledText("Synonyms", word.synonyms.joinToString(", "))
        }
        if (word.antonyms.isNotEmpty()) {
            LabeledText("Antonyms", word.antonyms.joinToString(", "))
        }
        if (word.example.isNotBlank()) {
            LabeledText("Example", "\u201C${word.example}\u201D", italic = true)
        }
    }
}

@Composable
private fun LabeledText(label: String, value: String, italic: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal
        )
    }
}
