package com.vocabulary.app.ui.quiz.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.core.content.res.ResourcesCompat
import com.vocabulary.app.R
import com.vocabulary.app.ui.theme.BANGLA_SCALE
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.ui.components.title
import com.vocabulary.app.ui.quiz.AnsweredQuestion
import com.vocabulary.app.ui.quiz.QuizSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val PAGE_W = 595f
const val PAGE_H = 842f

/** One A4 page as a list of draw operations. The same page is drawn on screen and into the PDF. */
class PaperPage {
    private val ops = ArrayList<(Canvas) -> Unit>()
    fun add(op: (Canvas) -> Unit) { ops += op }
    fun draw(canvas: Canvas) { ops.forEach { it(canvas) } }
}

/**
 * Lays a quiz result out on A4 pages. Text goes through StaticLayout, so Bangla is shaped
 * correctly and falls back to the system Bangla font.
 */
class PaperLayout(context: Context) {

    private val logo: Bitmap? = runCatching {
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)
    }.getOrNull()
    /** English text: Tinos, the Times New Roman look-alike (same metrics). */
    private val latin: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.tinos) }
        .getOrNull() ?: Typeface.SERIF

    /** Bangla text: Kalpurush, the same font the app uses for word content. */
    private val bangla: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.kalpurush) }
        .getOrNull() ?: Typeface.SERIF

    private val logoBg: Int = runCatching { context.getColor(R.color.ic_launcher_background) }
        .getOrDefault(ACCENT)

    /**
     * Measurements follow the Examcamp paper: 0.75in margins, 8mm column gap, 5mm between
     * questions (1mm = 2.835pt), colours #111111 / #0A7D43 / #C62828.
     */
    private companion object {
        const val MARGIN = 54f
        const val CONTENT_W = PAGE_W - 2 * MARGIN
        const val COLUMN_GAP = 22.7f
        /** Page number sits 7mm above the bottom edge; questions never go below it. */
        const val FOOTER_BOTTOM = 19.8f
        const val BOTTOM = PAGE_H - MARGIN - FOOTER_BOTTOM
        const val QUESTION_GAP = 14.2f
        const val OPT_INDENT = 8.5f
        const val OPT_GAP_X = 17f
        const val OPT_GAP_Y = 5.7f
        const val OPT_MARGIN = 4.25f
        const val BODY_SPACING = 1.3f
        const val ACCENT = 0xFFA44A49.toInt()
        const val INK = 0xFF111111.toInt()
        const val MUTED = 0xFF111111.toInt()
        const val LINE = 0xFFDCDCDC.toInt()
        const val GREEN = 0xFF0A7D43.toInt()
        const val RED = 0xFFC62828.toInt()
    }

    private fun Int.withAlpha(a: Float): Int = (this and 0x00FFFFFF) or ((a * 255).toInt() shl 24)

    private fun paint(
        size: Float,
        color: Int = INK,
        bold: Boolean = false,
        italic: Boolean = false
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(
            latin,
            when {
                bold && italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
        )
    }

    private fun layout(
        text: CharSequence,
        paint: TextPaint,
        width: Float,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        spacing: Float = 1.2f
    ): StaticLayout {
        val styled = withBanglaFont(text)
        return StaticLayout.Builder
            .obtain(styled, 0, styled.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(0f, spacing)
            .setIncludePad(false)
            .build()
    }

    /** Gives every Bangla run the Bangla font; the rest keeps the paint's font (Tinos). */
    private fun withBanglaFont(text: CharSequence): CharSequence {
        if (text.none { it.isBangla() }) return text
        val out = SpannableStringBuilder(text)
        var i = 0
        while (i < out.length) {
            if (!out[i].isBangla()) { i++; continue }
            val start = i
            var end = i + 1
            while (end < out.length) {
                val c = out[end]
                val joinsNext = (c.isWhitespace() || c in ",;:()'\"-") &&
                    end + 1 < out.length && out[end + 1].isBangla()
                if (c.isBangla() || joinsNext) end++ else break
            }
            out.setSpan(BanglaSpan(bangla), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            i = end
        }
        return out
    }

    private fun Char.isBangla() = this in '\u0980'..'\u09FF' || this == '\u200C' || this == '\u200D'

    private class BanglaSpan(private val tf: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(p: TextPaint) = apply(p)
        override fun updateMeasureState(p: TextPaint) = apply(p)
        private fun apply(p: TextPaint) {
            val bold = p.typeface?.isBold == true || p.isFakeBoldText
            p.typeface = tf
            p.textSize = p.textSize * BANGLA_SCALE
            if (bold) p.isFakeBoldText = true
        }
    }

    private fun PaperPage.text(l: StaticLayout, x: Float, y: Float) = add { c ->
        c.save(); c.translate(x, y); l.draw(c); c.restore()
    }

    private fun PaperPage.line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = 0.7f) {
        val p = Paint().apply { color = LINE; strokeWidth = width; isAntiAlias = true }
        add { c -> c.drawLine(x1, y1, x2, y2, p) }
    }

    fun build(summary: QuizSummary, opts: ReportOptions): List<PaperPage> {
        val pages = ArrayList<PaperPage>()
        val scale = opts.textSize.scale
        val stats = ReportStats.compute(summary.items, summary.durationSec, opts.negativeMark)
        val title = opts.title.ifBlank { defaultReportTitle(summary) }
        val groupText = opts.groupText.ifBlank { summary.groupLabel.orEmpty() }
        val statsOnly = opts.content == ReportContent.STATS_ONLY

        fun newPage(): PaperPage = PaperPage().also { pages += it }

        /** "Page 01/03", right aligned in the bottom margin (added last, when the count is known). */
        fun addFooters() {
            pages.forEachIndexed { i, pg ->
                val text = String.format(Locale.US, "Page %02d/%02d", i + 1, pages.size)
                val l = layout(text, paint(8.5f, INK.withAlpha(0.6f)), CONTENT_W, Layout.Alignment.ALIGN_OPPOSITE)
                pg.text(l, MARGIN, PAGE_H - FOOTER_BOTTOM - l.height)
            }
        }

        var page = newPage()

        // ---- Heading (first page only) ------------------------------------------------------
        var y = drawHeader(page, MARGIN, title, groupText, opts, stats, showStats = opts.showStats || statsOnly)

        // ---- Statistics-only extra: result by mode ----------------------------------------
        if (statsOnly) {
            drawModeTable(page, y, summary)
            addFooters()
            return pages
        }

        // ---- MCQ paper -------------------------------------------------------------------
        val numbered = summary.items.mapIndexed { i, item -> (i + 1) to item }
            .filter { opts.scope == PaperScope.ALL || !it.second.isCorrect }

        if (numbered.isEmpty()) {
            val l = layout("No questions to show.", paint(11f, INK.withAlpha(0.6f), italic = true), CONTENT_W, Layout.Alignment.ALIGN_CENTER)
            page.text(l, MARGIN, y + 10f)
            addFooters()
            return pages
        }

        val columns = opts.columns.coerceIn(1, 2)
        val colW = (CONTENT_W - COLUMN_GAP * (columns - 1)) / columns
        val blocks = numbered.map { (number, item) -> questionBlock(item, number, colW, opts, scale) }
        // Height of each block including the gap under it (the same bookkeeping Examcamp uses).
        val heights = blocks.map { it.height + QUESTION_GAP }

        /**
         * With two columns the first blocks go to the left column and the rest to the right one.
         * Of all the splits that fit, the one that makes the taller column shortest wins, so the
         * two columns end up about the same height.
         */
        fun bestSplit(run: List<Float>, capacity: Float): Int? {
            if (columns == 1) return if (run.sum() <= capacity) run.size else null
            val total = run.sum()
            var best: Int? = null
            var bestTall = Float.MAX_VALUE
            var left = 0f
            for (split in 0..run.size) {
                if (split > 0) left += run[split - 1]
                val right = total - left
                if (left <= capacity && right <= capacity) {
                    val tall = maxOf(left, right)
                    if (tall < bestTall) { bestTall = tall; best = split }
                }
            }
            return best
        }

        var cursor = 0
        var pageIndex = 0
        while (cursor < blocks.size) {
            if (pageIndex > 0) { page = newPage(); y = MARGIN }
            val capacity = BOTTOM - y

            // Grow the run while some split still fits; a page always keeps at least one question.
            var runEnd = cursor + 1
            var split = bestSplit(heights.subList(cursor, runEnd), capacity) ?: 1
            while (runEnd < blocks.size) {
                val candidate = bestSplit(heights.subList(cursor, runEnd + 1), capacity) ?: break
                runEnd += 1
                split = candidate
            }

            var col = 0
            var cy = y
            for (k in cursor until runEnd) {
                if (k - cursor == split && columns == 2) { col = 1; cy = y }
                val x = MARGIN + col * (colW + COLUMN_GAP)
                val block = blocks[k]
                val top = cy
                page.add { c -> block.draw(c, x, top) }
                cy += heights[k]
            }
            cursor = runEnd
            pageIndex++
        }
        addFooters()
        return pages
    }

    // ---- Header --------------------------------------------------------------------------

    /**
     * Subject and topic on the left, the logo and name on the right, then one line of result
     * figures separated by bars and a thin grey rule. Returns the y where the questions start.
     */
    private fun drawHeader(
        page: PaperPage,
        top: Float,
        title: String,
        groupText: String,
        opts: ReportOptions,
        stats: ReportStats,
        showStats: Boolean
    ): Float {
        var y = top
        val logoSize = 18.4f
        val brandPaint = paint(13f, ACCENT, bold = true)
        val brandTextW = brandPaint.measureText("Vocabox")
        val brandW = if (opts.showLogo) logoSize + 5f + brandTextW else 0f
        val date = if (opts.showDate) SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()) else null
        val dateW = if (date != null) 90f else 0f
        val rightW = maxOf(brandW, dateW)

        val titleW = if (rightW > 0f) CONTENT_W - rightW - COLUMN_GAP else CONTENT_W
        val titleL = layout(title, paint(15f, INK, bold = true), titleW)
        val topicL = if (groupText.isNotBlank())
            layout(groupText, paint(10.5f, INK.withAlpha(0.75f)), titleW) else null
        val titlesH = titleL.height + (topicL?.let { 2.8f + it.height } ?: 0f)

        val dateL = date?.let { layout(it, paint(8.5f, INK.withAlpha(0.6f)), rightW, Layout.Alignment.ALIGN_OPPOSITE) }
        val brandH = if (opts.showLogo) logoSize else 0f
        val rightH = brandH + (dateL?.let { (if (brandH > 0f) 3f else 0f) + it.height } ?: 0f)
        val rowH = maxOf(titlesH, rightH)

        val titlesTop = y + (rowH - titlesH) / 2f
        page.text(titleL, MARGIN, titlesTop)
        if (topicL != null) page.text(topicL, MARGIN, titlesTop + titleL.height + 2.8f)

        var ry = y + (rowH - rightH) / 2f
        if (opts.showLogo) {
            val bx = PAGE_W - MARGIN - brandW
            drawLogo(page, bx, ry, logoSize)
            val wm = layout("Vocabox", brandPaint, brandTextW + 4f)
            page.text(wm, bx + logoSize + 5f, ry + (logoSize - wm.height) / 2f)
            ry += logoSize + 3f
        }
        if (dateL != null) page.text(dateL, PAGE_W - MARGIN - rightW, ry)
        y += rowH

        if (showStats) {
            y += 8.5f
            y = drawStatsLine(page, y, stats)
            y += 8.5f
        } else {
            y += 8.5f
        }
        page.line(MARGIN, y, PAGE_W - MARGIN, y, 0.75f)
        return y + 17f
    }

    private fun drawLogo(page: PaperPage, x: Float, y: Float, size: Float) {
        val bmp = logo
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = logoBg }
        page.add { c ->
            c.drawRoundRect(RectF(x, y, x + size, y + size), size * 0.24f, size * 0.24f, bg)
            if (bmp != null) {
                // The launcher foreground has a safe-zone margin; crop to the middle.
                val inset = bmp.width * 0.17f
                val src = android.graphics.Rect(
                    inset.toInt(), inset.toInt(), (bmp.width - inset).toInt(), (bmp.height - inset).toInt()
                )
                c.drawBitmap(bmp, src, RectF(x, y, x + size, y + size), Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }
    }

    /** "Obtained Marks: 7 | Correct: 8 | Wrong: 1 | Skipped: 1 | Negative Marks: 0.25 | Percentage: 77%" */
    private fun drawStatsLine(page: PaperPage, top: Float, s: ReportStats): Float {
        val wrongAnswered = s.wrong - s.skipped
        val labels = listOf(
            "Obtained Marks: ${formatMarks(s.obtained)}",
            "Correct: ${s.correct}",
            "Wrong: $wrongAnswered",
            "Skipped: ${s.skipped}",
            "Negative Marks: ${formatMarks(s.negative)}",
            "Percentage: ${s.percent}%"
        )
        // One line, as large as 9pt but shrunk if the figures would not fit the page width.
        var size = 9f
        var boldP = paint(size, INK, bold = true)
        var sepP = paint(size, INK.withAlpha(0.4f))
        var widths = labels.map { boldP.measureText(it) } + List(labels.size - 1) { sepP.measureText("|") }
        while (widths.sum() > CONTENT_W && size > 6f) {
            size -= 0.25f
            boldP = paint(size, INK, bold = true)
            sepP = paint(size, INK.withAlpha(0.4f))
            widths = labels.map { boldP.measureText(it) } + List(labels.size - 1) { sepP.measureText("|") }
        }
        val itemCount = labels.size * 2 - 1
        val gap = ((CONTENT_W - widths.sum()) / (itemCount - 1)).coerceAtLeast(0f)
        val fm = boldP.fontMetrics
        val pad = 3.75f
        val baseline = top + pad - fm.ascent
        var x = MARGIN
        labels.forEachIndexed { i, label ->
            val lx = x
            page.add { c -> c.drawText(label, lx, baseline, boldP) }
            x += boldP.measureText(label) + gap
            if (i < labels.size - 1) {
                val sx = x
                page.add { c -> c.drawText("|", sx, baseline, sepP) }
                x += sepP.measureText("|") + gap
            }
        }
        return top + pad * 2 + (fm.descent - fm.ascent)
    }

    private fun drawModeTable(page: PaperPage, top: Float, summary: QuizSummary): Float {
        val byMode = QuizMode.entries.mapNotNull { mode ->
            val items = summary.items.filter { it.question.mode == mode }
            if (items.isEmpty()) null else Triple(mode, items.count { it.isCorrect }, items.size)
        }
        if (byMode.isEmpty()) return top
        var y = top + 6f
        val head = layout("Result by category", paint(11f, INK, bold = true), CONTENT_W)
        page.text(head, MARGIN, y)
        y += head.height + 6f
        val wName = CONTENT_W * 0.55f
        val wNum = (CONTENT_W - wName) / 3f
        fun row(cells: List<String>, bold: Boolean) {
            val p = paint(10f, INK, bold = bold)
            val ls = cells.mapIndexed { i, t ->
                layout(t, p, if (i == 0) wName - 6f else wNum - 6f,
                    if (i == 0) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER)
            }
            val h = ls.maxOf { it.height } + 8f
            var x = MARGIN
            ls.forEachIndexed { i, l ->
                page.text(l, x + 3f, y + 4f)
                x += if (i == 0) wName else wNum
            }
            y += h
            page.line(MARGIN, y, MARGIN + CONTENT_W, y)
        }
        row(listOf("Category", "Correct", "Total", "%"), true)
        byMode.forEach { (mode, ok, total) ->
            row(listOf(mode.title(), "$ok", "$total", "${ok * 100 / total}%"), false)
        }
        return y
    }

    // ---- Question block -----------------------------------------------------------------

    private class Block(val height: Float, val draw: (Canvas, Float, Float) -> Unit)

    /** Width of [text] on one line, used to decide how the options can be arranged. */
    private fun naturalWidth(text: CharSequence, paint: TextPaint): Float {
        val l = layout(text, paint, 100_000f)
        return if (l.lineCount > 0) l.getLineWidth(0) else 0f
    }

    /**
     * "01. Question" in bold, the options as "A. text" (right one green, a wrong pick red), and
     * the explanation under it. The options use the most compact arrangement whose texts all fit
     * on one line: all in a row, two by two, or one under the other.
     */
    private fun questionBlock(
        item: AnsweredQuestion,
        number: Int,
        width: Float,
        opts: ReportOptions,
        scale: Float
    ): Block {
        val q = item.question
        val showAnswers = opts.paperType != PaperType.QUESTIONS
        val showMine = showAnswers && opts.showMyAnswers
        val draws = ArrayList<(Canvas, Float, Float) -> Unit>()
        var h = 0f

        fun add(l: StaticLayout, dx: Float = 0f) {
            val top = h
            draws += { c, x, y -> c.save(); c.translate(x + dx, y + top); l.draw(c); c.restore() }
            h += l.height
        }

        val head = SpannableStringBuilder(String.format(Locale.US, "%02d. ", number))
        if (q.instruction.isNotBlank()) head.append("${q.instruction}: ")
        head.append(q.prompt)
        head.setSpan(StyleSpan(Typeface.BOLD), 0, head.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (q.hint.isNotBlank()) {
            val start = head.length
            head.append("  (${q.hint})")
            head.setSpan(StyleSpan(Typeface.ITALIC), start, head.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            head.setSpan(ForegroundColorSpan(INK.withAlpha(0.7f)), start, head.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            head.setSpan(RelativeSizeSpan(0.85f), start, head.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        add(layout(head, paint(10f * scale, INK), width, spacing = BODY_SPACING))
        h += OPT_MARGIN

        // Options: text and paint for each, then the arrangement.
        val avail = width - OPT_INDENT
        val texts = q.options.mapIndexed { i, option ->
            val isCorrect = showAnswers && i == q.correctIndex
            val isMine = showMine && item.selected == i && i != q.correctIndex
            val p = paint(
                10f * scale,
                when { isCorrect -> GREEN; isMine -> RED; else -> INK },
                bold = isCorrect || isMine
            )
            "${'A' + i}. $option" to p
        }
        fun fits(cols: Int): Boolean {
            val cellW = (avail - OPT_GAP_X * (cols - 1)) / cols
            return texts.all { (t, p) -> naturalWidth(t, p) + 1f <= cellW }
        }
        val cols = when {
            texts.size in 2..4 && fits(texts.size) -> texts.size
            texts.size > 2 && fits(2) -> 2
            else -> 1
        }
        val cellW = (avail - OPT_GAP_X * (cols - 1)) / cols
        texts.chunked(cols).forEachIndexed { r, row ->
            if (r > 0 && cols > 1) h += OPT_GAP_Y
            val ls = row.map { (t, p) -> layout(t, p, cellW, spacing = BODY_SPACING) }
            val rowTop = h
            ls.forEachIndexed { k, l ->
                val dx = OPT_INDENT + k * (cellW + OPT_GAP_X)
                draws += { c, x, y -> c.save(); c.translate(x + dx, y + rowTop); l.draw(c); c.restore() }
            }
            h += ls.maxOf { it.height }
        }

        if (opts.paperType == PaperType.WITH_EXPLANATION) {
            val lines = q.explanation.lines().filter { it.isNotBlank() }.map { it.replace("**", "") }
            if (lines.isNotEmpty()) {
                h += OPT_MARGIN
                val label = "Explanation: "
                val ex = SpannableStringBuilder(label)
                ex.setSpan(StyleSpan(Typeface.BOLD), 0, label.length - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ex.append(lines.joinToString("\n"))
                add(layout(ex, paint(10f * scale, INK.withAlpha(0.8f)), width, spacing = BODY_SPACING))
            }
        }

        return Block(h) { c, x, y -> draws.forEach { it(c, x, y) } }
    }
}
