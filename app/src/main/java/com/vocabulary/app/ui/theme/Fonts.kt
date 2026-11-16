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
        ranges.forEach { addStyle(SpanStyle(fontFamily = Kalpurush), it.first, it.last + 1) }
    }
}

/** Plain text of a string that uses **bold** markers. */
fun String.stripMarkup(): String = replace("**", "")

/** Text with **bold** markers: the markers are removed, the marked parts are bold, Bangla runs use Kalpurush. */
fun String.markup(): AnnotatedString {
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
        bengaliRanges(text).forEach { addStyle(SpanStyle(fontFamily = Kalpurush), it.first, it.last + 1) }
    }
}
