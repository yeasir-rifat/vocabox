package com.vocabulary.app.ui.components

import com.vocabulary.app.ui.theme.TimesNewRoman
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.ui.theme.bn

/**
 * Two lines that describe one entry in every list: "Abate verb" (the word, then a small light italic
 * part of speech) and the Bangla meaning under it. Set [showPartOfSpeech] to false when the row shows
 * something else in its place, such as a type badge.
 */
@Composable
fun WordSummary(
    word: WordEntity,
    modifier: Modifier = Modifier,
    showPartOfSpeech: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val posColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        Text(
            text = buildAnnotatedString {
                append(word.word)
                if (showPartOfSpeech) {
                    append(" ")
                    withStyle(
                        SpanStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            color = posColor
                        )
                    ) { append(word.partOfSpeech.shortPartOfSpeech()) }
                }
            },
            style = MaterialTheme.typography.titleMedium,
            fontFamily = TimesNewRoman,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = word.banglaMeaning.bn(),
            fontFamily = TimesNewRoman,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
