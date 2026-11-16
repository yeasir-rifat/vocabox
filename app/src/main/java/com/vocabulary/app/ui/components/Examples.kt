package com.vocabulary.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.vocabulary.app.data.local.entity.WordEntity

/**
 * The examples of a word. The data keeps them in one string, one example per line, so a word can
 * have one or several. A leading bullet in the data is ignored.
 */
fun String.exampleLines(): List<String> =
    lines().map { it.trim().removePrefix("•").trim() }.filter { it.isNotEmpty() }

fun WordEntity.exampleLines(): List<String> = example.exampleLines()

/** Examples as a bullet list, one bullet per example, without quotation marks. */
@Composable
fun ExampleBullets(
    example: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontFamily: FontFamily? = null,
    render: (String) -> AnnotatedString = { AnnotatedString(it) }
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        example.exampleLines().forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "•", style = style, fontFamily = fontFamily)
                Text(
                    text = render(line),
                    style = style,
                    fontFamily = fontFamily,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
