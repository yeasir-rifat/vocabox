package com.vocabulary.app.ui.theme

import android.os.Build
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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

/** Bangla text. */
val NotoSerifBengali = FontFamily(weights.map { variableFont(R.font.noto_serif_bengali, it) })

private fun Char.isBengali() = this in '\u0980'..'\u09FF' || this == '\u200C' || this == '\u200D'

/**
 * Marks every Bangla run with Noto Serif Bengali. Spaces and punctuation between Bangla
 * characters stay in the Bangla run; everything else keeps the style's font (DM Sans).
 */
fun String.bn(): AnnotatedString {
    if (none { it.isBengali() }) return AnnotatedString(this)
    return buildAnnotatedString {
        append(this@bn)
        var runStart = -1
        var lastBengali = -1
        for (i in this@bn.indices) {
            val c = this@bn[i]
            if (c.isBengali()) {
                if (runStart < 0) runStart = i
                lastBengali = i
            } else if (runStart >= 0 && !(c.isWhitespace() || c in ",;:()'\"-")) {
                addStyle(SpanStyle(fontFamily = NotoSerifBengali), runStart, lastBengali + 1)
                runStart = -1
            }
        }
        if (runStart >= 0) addStyle(SpanStyle(fontFamily = NotoSerifBengali), runStart, lastBengali + 1)
    }
}
