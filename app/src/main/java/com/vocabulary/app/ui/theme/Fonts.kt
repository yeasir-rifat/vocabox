package com.vocabulary.app.ui.theme

import android.os.Build
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.vocabulary.app.R

private val weights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold
)

/** Variable font weights need API 26+. Older devices show the regular weight. */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight): Font {
    val settings = if (Build.VERSION.SDK_INT >= 26) {
        FontVariation.Settings(FontVariation.weight(weight.weight))
    } else {
        FontVariation.Settings()
    }
    return Font(resId = resId, weight = weight, variationSettings = settings)
}

/** English text. */
val DmSans = FontFamily(weights.map { variableFont(R.font.dm_sans, it) })

/**
 * Word content (lists, flashcards, popups, details, quizzes) uses Times New Roman for English and
 * Kalpurush for Bangla. The app's own UI keeps DM Sans. Times New Roman comes with regular only, so
 * Tinos (metric compatible) supplies bold and italic.
 */
val TimesNewRoman = FontFamily(
    Font(R.font.times_new_roman, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.tinos_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.tinos_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.tinos_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

/** Bangla content text (Kalpurush has one weight; bold is synthesised). */
val Kalpurush = FontFamily(Font(R.font.kalpurush, FontWeight.Normal))

/**
 * Size of every Bangla run relative to the English text around it (app text and quiz PDF).
 *
 * Measured from the font files: Kalpurush draws its headline (matra) line at 0.644 em, Times New
 * Roman draws its ascenders at 0.683 em and capitals at 0.662 em. 0.683 / 0.644 = 1.06 puts the
 * Bangla headline level with the English ascender line, so 1.05 balances the two. Going higher
 * pushes the Bangla vowel signs (up to 0.995 em) above the line box of the tighter line heights.
 */
const val BANGLA_SCALE = 1.05f

private fun Char.isBengali() = this in '\u0980'..'\u09FF' || this == '\u200C' || this == '\u200D'

/** Ranges of Bangla runs. Spaces and punctuation between Bangla characters stay inside the run. */
private fun bengaliRanges(text: String): List<IntRange> {
    val ranges = ArrayList<IntRange>()
    var runStart = -1
    var lastBengali = -1
    for (i in text.indices) {
        val c = text[i]
        if (c.isBengali()) {
            if (runStart < 0) runStart = i
            lastBengali = i
        } else if (runStart >= 0 && !(c.isWhitespace() || c in ",;:()'\"-")) {
            ranges += runStart..lastBengali
            runStart = -1
        }
    }
    if (runStart >= 0) ranges += runStart..lastBengali
    return ranges
}

/**
 * Marks every Bangla run with Kalpurush. Spaces and punctuation between Bangla
 * characters stay in the Bangla run; everything else keeps the style's font.
 */
fun String.bn(): AnnotatedString {
    val ranges = bengaliRanges(this)
    if (ranges.isEmpty()) return AnnotatedString(this)
    return buildAnnotatedString {
        append(this@bn)
        ranges.forEach { addStyle(SpanStyle(fontFamily = Kalpurush, fontSize = BANGLA_SCALE.em), it.first, it.last + 1) }
    }
}

/**
 * Bangla runs that sit next to regular-weight Times New Roman (synonym and antonym lines).
 *
 * Kalpurush's vertical stems are thinner than Times New Roman's (0.054 em against 0.081 em), but its
 * wide curves and heavy headline line (matra) carry the weight, and rendered side by side the regular
 * weight matches the English. Synthetic bold was tried and looked far too heavy, so the weight stays
 * regular. Only the colour (same as English) and the size are matched; the size is a little below BANGLA_SCALE.
 */
const val BANGLA_MATCHED_SCALE = 0.93f

fun String.bnMatched(): AnnotatedString {
    val ranges = bengaliRanges(this)
    if (ranges.isEmpty()) return AnnotatedString(this)
    return buildAnnotatedString {
        append(this@bnMatched)
        ranges.forEach {
            addStyle(SpanStyle(fontFamily = Kalpurush, fontSize = BANGLA_MATCHED_SCALE.em), it.first, it.last + 1)
        }
    }
}

/** Plain text of a string that uses **bold** markers. */
fun String.stripMarkup(): String = replace("**", "")

/** Text with **bold** markers: the markers are removed, the marked parts are bold, Bangla runs use Kalpurush. */
fun String.markup(banglaScale: Float = BANGLA_SCALE): AnnotatedString {
    val plain = StringBuilder()
    val bold = ArrayList<IntRange>()
    var open = -1
    var i = 0
    while (i < length) {
        if (startsWith("**", i)) {
            if (open < 0) open = plain.length else { bold += open until plain.length; open = -1 }
            i += 2
        } else {
            plain.append(this[i]); i++
        }
    }
    val text = plain.toString()
    return buildAnnotatedString {
        append(text)
        bold.forEach { addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.first, it.last + 1) }
        bengaliRanges(text).forEach { addStyle(SpanStyle(fontFamily = Kalpurush, fontSize = banglaScale.em), it.first, it.last + 1) }
    }
}

/**
 * One line of a quiz explanation. The "Synonyms:" and "Antonyms:" lines carry Bangla meanings in
 * brackets, which use the same smaller Bangla size as the synonym lines everywhere else.
 */
fun String.explanationMarkup(): AnnotatedString =
    if (startsWith("**Synonyms:**") || startsWith("**Antonyms:**")) markup(BANGLA_MATCHED_SCALE) else markup()
