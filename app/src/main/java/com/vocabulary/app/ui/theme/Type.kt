package com.vocabulary.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale (DM Sans; Bangla runs are switched to Kalpurush by String.bn()).
 *
 *  displaySmall   32/40  vocabulary word on flashcards and word detail
 *  headlineMedium 26/34  screen titles
 *  headlineSmall  22/30  section emphasis, streak count
 *  titleLarge     20/28  dialog and result titles
 *  titleMedium    16/24  card headings, list words, Bangla meanings
 *  titleSmall     14/20  small headings, group labels
 *  bodyLarge      16/26  definitions and example sentences (extra line height for Bangla)
 *  bodyMedium     14/22  supporting text (22sp line height leaves room for Bangla ascenders)
 *  bodySmall      12/18  captions
 *  labelLarge     14/20  buttons, chips
 *  labelMedium    12/16  badges
 *  labelSmall     11/16  overlines, chart labels
 */
private val BaseTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

private fun TextStyle.dm() = copy(fontFamily = DmSans)

/** Material defaults for the styles not set above, with DM Sans as the font. */
val AppTypography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.dm(),
    displayMedium = BaseTypography.displayMedium.dm(),
    headlineLarge = BaseTypography.headlineLarge.dm(),
    titleSmall = BaseTypography.titleSmall.dm()
)
