package com.vocabulary.app.ui.quiz.report

import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.domain.QuizQuestion
import com.vocabulary.app.ui.quiz.AnsweredQuestion
import com.vocabulary.app.ui.quiz.UNANSWERED
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportStatsTest {
    private fun q() = QuizQuestion(1, QuizMode.MEANING, "i", "p", "", listOf("a", "b", "c", "d"), 0, "e")

    @Test
    fun negativeMarksOnlyForAnsweredWrong() {
        val items = listOf(
            AnsweredQuestion(q(), 0), // correct
            AnsweredQuestion(q(), 0), // correct
            AnsweredQuestion(q(), 1), // wrong
            AnsweredQuestion(q(), 2), // wrong
            AnsweredQuestion(q(), UNANSWERED) // skipped
        )
        val s = ReportStats.compute(items, 75, 0.25f)
        assertEquals(2, s.correct)
        assertEquals(3, s.wrong)
        assertEquals(1, s.skipped)
        assertEquals(0.5, s.negative, 1e-9)
        assertEquals(1.5, s.obtained, 1e-9)
        assertEquals(30, s.percent)
    }

    @Test
    fun percentNeverBelowZeroAndFormatting() {
        val s = ReportStats.compute(listOf(AnsweredQuestion(q(), 1)), 5, 1f)
        assertEquals(0, s.percent)
        assertEquals("-1", formatMarks(s.obtained))
        assertEquals("0.25", formatMarks(0.25))
        assertEquals("5", formatMarks(5.0))
        assertEquals("1m 5s", formatDuration(65))
    }
}
