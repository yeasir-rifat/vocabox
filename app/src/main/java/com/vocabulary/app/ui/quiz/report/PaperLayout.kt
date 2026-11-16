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

    /** Bangla text: Noto Serif Bengali, the same font the app uses. */
    private val bangla: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.noto_serif_bengali) }
        .getOrNull() ?: Typeface.SERIF

    private val logoBg: Int = runCatching { context.getColor(R.color.ic_launcher_background) }
        .getOrDefault(ACCENT)

    private companion object {
        const val MARGIN = 36f
        const val CONTENT_W = PAGE_W - 2 * MARGIN
        const val COLUMN_GAP = 20f
        const val BOTTOM = PAGE_H - MARGIN - 8f
        const val ACCENT = 0xFFA44A49.toInt()
        const val INK = 0xFF2A1B1A.toInt()
        const val MUTED = 0xFF6B5B58.toInt()
        const val LINE = 0xFFC9BDB1.toInt()
        const val GREEN = 0xFF15803D.toInt()
        const val RED = 0xFFDC2626.toInt()
        const val TINT = 0xFFF6EFE8.toInt()
    }

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
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        val styled = withBanglaFont(text)
        return StaticLayout.Builder
            .obtain(styled, 0, styled.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(align)
            .setLineSpacing(0f, 1.2f)
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

        fun newPage(): PaperPage {
            val page = PaperPage()
            pages += page
            val footer = layout(
                "Vocabox  \u00B7  Page ${pages.size}", paint(8f, MUTED), CONTENT_W,
                Layout.Alignment.ALIGN_CENTER
            )
            page.text(footer, MARGIN, PAGE_H - MARGIN + 4f)
            return page
        }

        var page = newPage()
        var y = MARGIN

        // ---- Heading ---------------------------------------------------------------------
        val logoSize = 38f
        var brandX = MARGIN
        if (opts.showLogo) {
            val bmp = logo
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = logoBg }
            val startY = y
            page.add { c ->
                c.drawRoundRect(RectF(MARGIN, startY, MARGIN + logoSize, startY + logoSize), 9f, 9f, bg)
                if (bmp != null) {
                    // The launcher foreground has a safe-zone margin; crop to the middle.
                    val inset = bmp.width * 0.17f
                    val src = android.graphics.Rect(
                        inset.toInt(), inset.toInt(), (bmp.width - inset).toInt(), (bmp.height - inset).toInt()
                    )
                    c.drawBitmap(
                        bmp, src,
                        RectF(MARGIN, startY, MARGIN + logoSize, startY + logoSize),
                        Paint(Paint.FILTER_BITMAP_FLAG)
                    )
                }
            }
            brandX += logoSize + 10f
        }
        val brand = layout("Vocabox", paint(24f, ACCENT, bold = true), 200f)
        page.text(brand, brandX, y + (logoSize - brand.height) / 2f)
        if (opts.showDate) {
            val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())
            val dl = layout(date, paint(9f, MUTED), 150f, Layout.Alignment.ALIGN_OPPOSITE)
            page.text(dl, PAGE_W - MARGIN - 150f, y + (logoSize - dl.height) / 2f)
        }
        y += logoSize + 10f
        page.line(MARGIN, y, PAGE_W - MARGIN, y, 1.2f)
        y += 12f

        val titleL = layout(title, paint(17f, INK, bold = true), CONTENT_W, Layout.Alignment.ALIGN_CENTER)
        page.text(titleL, MARGIN, y)
        y += titleL.height + 3f
        if (groupText.isNotBlank()) {
            val gl = layout(groupText, paint(11f, MUTED), CONTENT_W, Layout.Alignment.ALIGN_CENTER)
            page.text(gl, MARGIN, y)
            y += gl.height + 3f
        }
        y += 8f

        if (opts.showStats || statsOnly) {
            y = drawStatsTable(page, y, stats)
            if (stats.skipped > 0) {
                val note = layout(
                    "Wrong includes ${stats.skipped} unanswered question${if (stats.skipped == 1) "" else "s"} (no negative mark).",
                    paint(8f, MUTED, italic = true), CONTENT_W
                )
                page.text(note, MARGIN, y + 3f)
                y += note.height + 6f
            }
            y += 8f
        }

        // ---- Statistics-only extra: result by mode --------------------------------------
        if (statsOnly) {
            y = drawModeTable(page, y, summary)
            return pages
        }

        // ---- MCQ paper -------------------------------------------------------------------
        page.line(MARGIN, y, PAGE_W - MARGIN, y, 0.7f)
        y += 10f

        val columns = opts.columns.coerceIn(1, 2)
        val colW = (CONTENT_W - COLUMN_GAP * (columns - 1)) / columns
        var colTop = y
        var cy = y
        var col = 0
        var usedBottom = y

        fun closePage() {
            if (columns == 2) {
                page.line(MARGIN + colW + COLUMN_GAP / 2f, colTop, MARGIN + colW + COLUMN_GAP / 2f, usedBottom, 0.5f)
            }
        }

        fun nextSlot() {
            col++
            if (col >= columns) {
                closePage()
                page = newPage()
                val running = layout("Vocabox  \u00B7  $title", paint(8.5f, MUTED), CONTENT_W)
                page.text(running, MARGIN, MARGIN - 6f)
                page.line(MARGIN, MARGIN + running.height - 1f, PAGE_W - MARGIN, MARGIN + running.height - 1f, 0.5f)
                colTop = MARGIN + running.height + 8f
                col = 0
            }
            cy = colTop
            usedBottom = colTop
        }

        val numbered = summary.items.mapIndexed { i, item -> (i + 1) to item }
            .filter { opts.scope == PaperScope.ALL || !it.second.isCorrect }

        if (numbered.isEmpty()) {
            val l = layout("No questions to show.", paint(11f, MUTED, italic = true), CONTENT_W, Layout.Alignment.ALIGN_CENTER)
            page.text(l, MARGIN, cy + 10f)
            return pages
        }

        for ((number, item) in numbered) {
            val block = questionBlock(item, number, colW, opts, scale)
            if (cy + block.height > BOTTOM && cy > colTop) nextSlot()
            val x = MARGIN + col * (colW + COLUMN_GAP)
            val top = cy
            page.add { c -> block.draw(c, x, top) }
            cy += block.height + 10f
            usedBottom = maxOf(usedBottom, cy - 6f)
        }
        closePage()
        return pages
    }

    // ---- Statistics ---------------------------------------------------------------------

    private fun drawStatsTable(page: PaperPage, top: Float, s: ReportStats): Float {
        val cells = listOf(
            Triple("Correct", s.correct.toString(), GREEN),
            Triple("Wrong", s.wrong.toString(), RED),
            Triple("Marks obtained", "${formatMarks(s.obtained)} / ${s.total}", INK),
            Triple("Negative marks", if (s.negative > 0) "-" + formatMarks(s.negative) else "0", RED),
            Triple("Time", formatDuration(s.durationSec), INK),
            Triple("Percentage", "${s.percent}%", ACCENT)
        )
        val cols = 3
        val rows = 2
        val cellW = CONTENT_W / cols
        val cellH = 44f
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LINE; style = Paint.Style.STROKE; strokeWidth = 0.9f
        }
        val fill = Paint().apply { color = TINT }
        page.add { c ->
            c.drawRect(MARGIN, top, MARGIN + CONTENT_W, top + cellH * rows, fill)
            c.drawRect(MARGIN, top, MARGIN + CONTENT_W, top + cellH * rows, grid)
            for (i in 1 until cols) c.drawLine(MARGIN + cellW * i, top, MARGIN + cellW * i, top + cellH * rows, grid)
            for (r in 1 until rows) c.drawLine(MARGIN, top + cellH * r, MARGIN + CONTENT_W, top + cellH * r, grid)
        }
        cells.forEachIndexed { i, (label, value, color) ->
            val x = MARGIN + (i % cols) * cellW
            val y = top + (i / cols) * cellH
            val ll = layout(label, paint(8.5f, MUTED), cellW, Layout.Alignment.ALIGN_CENTER)
            val vl = layout(value, paint(15f, color, bold = true), cellW, Layout.Alignment.ALIGN_CENTER)
            page.text(ll, x, y + 7f)
            page.text(vl, x, y + 7f + ll.height + 3f)
        }
        return top + cellH * rows
    }

    private fun drawModeTable(page: PaperPage, top: Float, summary: QuizSummary): Float {
        val byMode = QuizMode.entries.mapNotNull { mode ->
            val items = summary.items.filter { it.question.mode == mode }
            if (items.isEmpty()) null else Triple(mode, items.count { it.isCorrect }, items.size)
        }
        if (byMode.isEmpty()) return top
        var y = top + 6f
        val head = layout("Result by category", paint(11f, ACCENT, bold = true), CONTENT_W)
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

        if (q.instruction.isNotBlank()) {
            add(layout(q.instruction, paint(8f * scale, MUTED, italic = true), width))
            h += 1f
        }

        val prompt = SpannableStringBuilder("$number. ${q.prompt}")
        prompt.setSpan(StyleSpan(Typeface.BOLD), 0, prompt.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (q.hint.isNotBlank()) {
            val start = prompt.length
            prompt.append("  (${q.hint})")
            prompt.setSpan(StyleSpan(Typeface.ITALIC), start, prompt.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            prompt.setSpan(ForegroundColorSpan(MUTED), start, prompt.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            prompt.setSpan(RelativeSizeSpan(0.85f), start, prompt.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        add(layout(prompt, paint(10f * scale, INK), width))
        h += 4f

        val radius = 6.5f * scale
        val indent = radius * 2 + 6f
        q.options.forEachIndexed { i, option ->
            val isCorrect = i == q.correctIndex
            val isMine = showMine && item.selected == i && !isCorrect
            val l = layout(option, paint(10f * scale, INK, bold = showAnswers && isCorrect), width - indent)
            val top = h
            val fill = when {
                showAnswers && isCorrect -> GREEN
                isMine -> RED
                else -> null
            }
            val letter = ('A' + i).toString()
            val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                if (fill != null) { color = fill; style = Paint.Style.FILL }
                else { color = MUTED; style = Paint.Style.STROKE; strokeWidth = 0.9f }
            }
            val letterPaint = paint(7.5f * scale, if (fill != null) 0xFFFFFFFF.toInt() else INK, bold = true)
                .apply { textAlign = Paint.Align.CENTER }
            draws += { c, x, y ->
                val cy = y + top + (l.getLineBottom(0) / 2f)
                c.drawCircle(x + radius, cy, radius, circle)
                val fm = letterPaint.fontMetrics
                c.drawText(letter, x + radius, cy - (fm.ascent + fm.descent) / 2f, letterPaint)
                c.save(); c.translate(x + indent, y + top); l.draw(c); c.restore()
            }
            h += l.height + 3f
        }

        if (showAnswers) {
            val letter = ('A' + q.correctIndex).toString()
            val line = SpannableStringBuilder("Answer: $letter")
            line.setSpan(StyleSpan(Typeface.BOLD), 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            line.setSpan(ForegroundColorSpan(GREEN), 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (showMine) {
                val mine = when {
                    item.selected < 0 -> "not answered"
                    item.isCorrect -> null
                    else -> ('A' + item.selected).toString()
                }
                if (mine != null) {
                    val s = line.length
                    line.append("    Your answer: $mine")
                    line.setSpan(ForegroundColorSpan(RED), s, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            add(layout(line, paint(10f * scale, INK), width))
        }

        if (opts.paperType == PaperType.WITH_EXPLANATION) {
            h += 4f
            val pad = 5f
            val ex = SpannableStringBuilder()
            q.explanation.lines().filter { it.isNotBlank() }.forEachIndexed { idx, ln ->
                if (idx > 0) ex.append('\n')
                val start = ex.length
                ex.append(ln)
                val boldEnd = if (idx == 0) ex.length else {
                    val colon = ln.indexOf(':')
                    if (colon in 1..12) start + colon + 1 else start
                }
                if (boldEnd > start) ex.setSpan(StyleSpan(Typeface.BOLD), start, boldEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val l = layout(ex, paint(10f * scale, INK), width - pad * 2 - 3f)
            val top = h
            val boxH = l.height + pad * 2
            val bg = Paint().apply { color = TINT }
            val bar = Paint().apply { color = ACCENT }
            draws += { c, x, y ->
                c.drawRect(x, y + top, x + width, y + top + boxH, bg)
                c.drawRect(x, y + top, x + 2f, y + top + boxH, bar)
                c.save(); c.translate(x + pad + 3f, y + top + pad); l.draw(c); c.restore()
            }
            h += boxH
        }

        return Block(h) { c, x, y -> draws.forEach { it(c, x, y) } }
    }
}
