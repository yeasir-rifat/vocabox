package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.ProgressEntity
import com.vocabulary.app.data.model.WordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SpacedRepetitionTest {

    private val now = 1_760_000_000_000L // fixed point in time

    private fun correct(p: ProgressEntity?) = SpacedRepetition.review(p, 1L, known = true, now = now)
    private fun wrong(p: ProgressEntity?) = SpacedRepetition.review(p, 1L, known = false, now = now)

    @Test
    fun firstCorrectAnswerSchedulesOneDay() {
        val p = correct(null)
        assertEquals(1, p.intervalDays)
        assertEquals(WordStatus.LEARNING, p.status)
        assertEquals(1, p.correctCount)
        assertEquals(now, p.lastReviewedAt)
    }

    @Test
    fun intervalGrowsOneThreeThenByEaseFactor() {
        val first = correct(null)
        val second = correct(first)
        val third = correct(second)
        assertEquals(1, first.intervalDays)
        assertEquals(3, second.intervalDays)
        assertEquals(8, third.intervalDays) // 3 x 2.6, rounded
    }

    @Test
    fun wordBecomesMasteredAfterLongInterval() {
        var p: ProgressEntity? = null
        repeat(4) { p = correct(p) }
        assertTrue(p!!.intervalDays >= SpacedRepetition.MASTERED_INTERVAL_DAYS)
        assertEquals(WordStatus.MASTERED, p!!.status)
    }

    @Test
    fun wrongAnswerResetsIntervalAndLowersEase() {
        var p: ProgressEntity? = null
        repeat(3) { p = correct(p) }
        val missed = wrong(p)
        assertEquals(1, missed.intervalDays)
        assertEquals(WordStatus.LEARNING, missed.status)
        assertEquals(1, missed.wrongCount)
        assertTrue(missed.easeFactor < p!!.easeFactor)
    }

    @Test
    fun easeFactorNeverDropsBelowMinimum() {
        var p: ProgressEntity? = null
        repeat(20) { p = wrong(p) }
        assertEquals(SpacedRepetition.MIN_EASE, p!!.easeFactor, 0.0001f)
    }

    @Test
    fun nextReviewIsLocalMidnightOnTheRightDay() {
        val p = correct(correct(null)) // interval 3 days
        val cal = Calendar.getInstance().apply { timeInMillis = p.nextReviewAt }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))

        val today = Calendar.getInstance().apply { timeInMillis = now }
        today.add(Calendar.DAY_OF_YEAR, 3)
        assertEquals(today.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.DAY_OF_YEAR))
        assertTrue(p.nextReviewAt > now)
    }
}
