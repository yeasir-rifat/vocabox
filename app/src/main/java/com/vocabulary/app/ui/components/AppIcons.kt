package com.vocabulary.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Vocabox icon set: one consistent family of thin, round-capped line icons on a 24 x 24 grid
 * (1.75 stroke), replacing the stock Material filled glyphs. Filled variants exist only for
 * "active" states (saved star, selected home, streak flame).
 *
 * Icons are drawn in black and recolored by Icon(tint = ...) like any other ImageVector.
 * Directional icons set autoMirror so they flip in right-to-left layouts.
 */
object AppIcons {

    private const val STROKE = 1.75f

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M ${cx - r} $cy a $r $r 0 1 0 ${2 * r} 0 a $r $r 0 1 0 ${-2 * r} 0 z"

    private fun icon(
        name: String,
        vararg paths: String,
        filled: Boolean = false,
        autoMirror: Boolean = false
    ): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror
        )
        paths.forEach { path ->
            builder.addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    // Navigation and chrome
    private const val HOME =
        "M3 11 l9 -8 9 8 v9 a1 1 0 0 1 -1 1 h-5 v-6 h-6 v6 H4 a1 1 0 0 1 -1 -1 z"
    private const val STAR =
        "M12 3 l2.7 5.6 6.1 0.9 -4.4 4.3 1 6.1 L12 17 l-5.4 2.9 1 -6.1 L3.2 9.5 l6.1 -0.9 z"
    private const val FLAME =
        "M12 2 c1 3.5 5 5.5 5 10 a5 5 0 0 1 -10 0 c0 -1.8 0.8 -3 1.7 -4 0.2 1.3 0.9 2 1.8 2.2 C10 7 10.5 4.5 12 2 z"

    val Home = icon("Home", HOME)
    val HomeFilled = icon("HomeFilled", HOME, filled = true)
    val List = icon(
        "List",
        "M8 6 h13", "M8 12 h13", "M8 18 h13", "M3.5 6 h0.01", "M3.5 12 h0.01", "M3.5 18 h0.01",
        autoMirror = true
    )
    val Grid = icon(
        "Grid",
        "M4 4 h6 v6 H4 z", "M14 4 h6 v6 h-6 z", "M4 14 h6 v6 H4 z", "M14 14 h6 v6 h-6 z"
    )
    val Pencil = icon(
        "Pencil",
        "M12 20 h9",
        "M16.5 3.5 a2.1 2.1 0 0 1 3 3 L7 19 l-4 1 1 -4 z"
    )
    val Star = icon("Star", STAR)
    val StarFilled = icon("StarFilled", STAR, filled = true)
    val Settings = icon(
        "Settings",
        "M4 6 h9", "M17 6 h3", circle(15f, 6f, 2f),
        "M4 12 h3", "M11 12 h9", circle(9f, 12f, 2f),
        "M4 18 h11", "M19 18 h1", circle(17f, 18f, 2f)
    )

    val Shuffle = icon(
        "Shuffle",
        "M16 3 h5 v5", "M4 20 L21 3", "M21 16 v5 h-5", "M15 15 l6 6", "M4 4 l5 5"
    )

    // Actions
    val ArrowBack = icon("ArrowBack", "M19 12 H5", "M12 19 l-7 -7 7 -7", autoMirror = true)
    val ChevronLeft = icon("ChevronLeft", "M15 6 l-6 6 6 6", autoMirror = true)
    val ChevronRight = icon("ChevronRight", "M9 6 l6 6 -6 6", autoMirror = true)
    val ChevronDown = icon("ChevronDown", "M6 9 l6 6 6 -6")
    val Close = icon("Close", "M18 6 L6 18", "M6 6 l12 12")
    val Check = icon("Check", "M20 6 L9 17 l-5 -5")
    val Add = icon("Add", "M12 5 v14", "M5 12 h14")
    val Remove = icon("Remove", "M5 12 h14")
    val Search = icon("Search", circle(11f, 11f, 7.5f), "M21 21 l-4.7 -4.7")
    val SearchOff = icon("SearchOff", circle(11f, 11f, 7.5f), "M21 21 l-4.7 -4.7", "M5 5 l12 12")
    val Volume = icon(
        "Volume",
        "M11 5 L6 9 H2 v6 h4 l5 4 z",
        "M15.5 8.5 a5 5 0 0 1 0 7",
        "M19 5 a10 10 0 0 1 0 14",
        autoMirror = true
    )

    // Content
    val Cards = icon(
        "Cards",
        "M5 6 h10 a2 2 0 0 1 2 2 v10 a2 2 0 0 1 -2 2 H5 a2 2 0 0 1 -2 -2 V8 a2 2 0 0 1 2 -2 z",
        "M7 3 h12 a2 2 0 0 1 2 2 v12"
    )
    val BarChart = icon("BarChart", "M5 21 V10", "M12 21 V4", "M19 21 v-7")
    val CheckCircle = icon("CheckCircle", circle(12f, 12f, 9f), "M8 12.5 l2.7 2.7 L16 9.5")
    val AlertCircle = icon("AlertCircle", circle(12f, 12f, 9f), "M12 7.5 v5.5", "M12 16.5 v0.01")
    val Flame = icon("Flame", FLAME, filled = true)

    // Settings
    val User = icon("User", "M20 21 v-2 a4 4 0 0 0 -4 -4 H8 a4 4 0 0 0 -4 4 v2", circle(12f, 7f, 4f))
    val Moon = icon("Moon", "M21 12.8 A9 9 0 1 1 11.2 3 a7 7 0 0 0 9.8 9.8 z")
    val Bell = icon(
        "Bell",
        "M18 8 A6 6 0 0 0 6 8 c0 7 -3 9 -3 9 h18 s-3 -2 -3 -9",
        "M13.7 21 a2 2 0 0 1 -3.4 0"
    )
    val Clock = icon("Clock", circle(12f, 12f, 9f), "M12 7 v5 l3 2")
    val Trash = icon(
        "Trash",
        "M3 6 h18",
        "M19 6 v14 a2 2 0 0 1 -2 2 H7 a2 2 0 0 1 -2 -2 V6",
        "M8 6 V4 a2 2 0 0 1 2 -2 h4 a2 2 0 0 1 2 2 v2"
    )
    val Download = icon("Download", "M12 3 v12", "M7 10 l5 5 5 -5", "M5 21 h14")
    val Upload = icon("Upload", "M12 15 v-12", "M7 8 l5 -5 5 5", "M5 21 h14")
    val Shield = icon("Shield", "M12 3 l8 3 v6 c0 5 -3.5 8 -8 9 c-4.5 -1 -8 -4 -8 -9 v-6 z", "M9 12 l2 2 4 -4")
    val Tune = icon("Tune", "M4 6 h8", circle(15f, 6f, 2.5f), "M18 6 h2", "M4 12 h2", circle(9f, 12f, 2.5f), "M12 12 h8", "M4 18 h9", circle(16f, 18f, 2.5f), "M19 18 h1")
    val Help = icon(
        "Help",
        circle(12f, 12f, 9f),
        "M9.5 9 a2.5 2.5 0 0 1 4.7 -1.2 c0.5 0.9 0.1 1.5 -0.6 2.1 -0.7 0.6 -1.3 1 -1.4 2.1",
        "M12 16.5 v0.01"
    )
    val Info = icon("Info", circle(12f, 12f, 9f), "M12 11 v5.5", "M12 7.5 v0.01")
    val Target = icon("Target", circle(12f, 12f, 9f), circle(12f, 12f, 5f), "M12 12 h0.01")
}
