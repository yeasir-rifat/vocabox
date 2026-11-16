package com.vocabulary.app.ui.list

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Vertical A-Z strip. Tap or drag on it to jump to a letter.
 * Letters without any word are dimmed; [currentLetter] is highlighted.
 */
@Composable
fun AlphabetIndex(
    letters: List<Char>,
    enabledLetters: Set<Char>,
    currentLetter: Char?,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var heightPx by remember { mutableIntStateOf(1) }

    fun selectAt(y: Float) {
        if (letters.isEmpty()) return
        val index = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        onLetterSelected(letters[index])
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(36.dp)
            .semantics { contentDescription = "Alphabet index. Tap or drag to jump to a letter." }
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                detectTapGestures { offset -> selectAt(offset.y) }
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> selectAt(offset.y) },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        selectAt(change.position.y)
                    }
                )
            }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            val isCurrent = letter == currentLetter
            val color = when {
                isCurrent -> MaterialTheme.colorScheme.primary
                letter in enabledLetters -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            }
            Text(
                text = letter.toString(),
                color = color,
                style = TextStyle(
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}
