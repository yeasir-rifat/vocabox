package com.vocabulary.app.ui.quiz.report

import com.vocabulary.app.ui.components.title
import com.vocabulary.app.ui.quiz.AnsweredQuestion
import com.vocabulary.app.ui.quiz.QuizSummary
import java.util.Locale

enum class ReportContent { STATS_ONLY, PAPER }

enum class PaperType { QUESTIONS, WITH_ANSWERS, WITH_EXPLANATION }

enum class PaperScope { ALL, WRONG_ONLY }

enum class ReportTextSize(val scale: Float) { SMALL(0.9f), MEDIUM(1f), LARGE(1.15f) }

/** Everything the user can change in the PDF preview. */
data class ReportOptions(
    val content: ReportContent = ReportContent.PAPER,
    val paperType: PaperType = PaperType.WITH_ANSWERS,
    val scope: PaperScope = PaperScope.ALL,
    /** Mark the option the user picked (red when wrong). Only used when answers are shown. */
    val showMyAnswers: Boolean = true,
    val columns: Int = 2,
    val textSize: ReportTextSize = ReportTextSize.MEDIUM,
    /** Blank = use the default (mode name / "Mixed Quiz"). */
    val title: String = "",
    /** Group no. or any other note. Blank = the group name when the quiz came from a group. */
    val groupText: String = "",
    val showLogo: Boolean = true,
    val showDate: Boolean = true,
    val showStats: Boolean = true,
    /** Marks taken off for each wrong (answered) question. */
    val negativeMark: Float = 0.25f
)

data class ReportStats(
    val total: Int,
    val correct: Int,
    /** Everything that is not correct, including unanswered questions (same as the rest of the app). */
    val wrong: Int,
    val skipped: Int,
    val negative: Double,
    /** Correct answers minus the negative marks. Can be below zero. */
    val obtained: Double,
    val percent: Int,
    val durationSec: Int
) {
    companion object {
        fun compute(items: List<AnsweredQuestion>, durationSec: Int, negativeMark: Float): ReportStats {
            val total = items.size
            val correct = items.count { it.isCorrect }
            val skipped = items.count { it.selected < 0 }
            val wrongAnswered = total - correct - skipped
            val negative = wrongAnswered * negativeMark.toDouble()
            val obtained = correct - negative
            val percent = if (total == 0) 0 else (obtained / total * 100).coerceAtLeast(0.0).toInt()
            return ReportStats(total, correct, total - correct, skipped, negative, obtained, percent, durationSec)
        }
    }
}

/** 5 -> "5", 4.75 -> "4.75", 4.5 -> "4.5". */
fun formatMarks(value: Double): String {
    val text = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    return if (text == "-0" || text.isEmpty()) "0" else text
}

fun formatDuration(sec: Int): String = "${sec / 60}m ${sec % 60}s"

fun defaultReportTitle(summary: QuizSummary): String =
    if (summary.modes.size == 1) summary.modes.first().title() else "Mixed Quiz"
