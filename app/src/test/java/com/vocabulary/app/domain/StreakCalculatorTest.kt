package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.DailyActivityEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private fun day(d: Long, reviewed: Int, goal: Int = 10) =
        DailyActivityEntity(day = d, reviewed = reviewed, correct = reviewed, goal = goal)

    @Test
    fun noActivityMeansNoStreak() {
        assertEquals(StreakInfo(0, 0), StreakCalculator.calculate(emptyList(), today = 100))
    }

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val days = listOf(day(98, 10), day(99, 12), day(100, 10))
        assertEquals(3, StreakCalculator.calculate(days, today = 100).current)
    }

    @Test
    fun streakSurvivesUntilTodayEndsWithoutReachingTheGoal() {
        val days = listOf(day(98, 10), day(99, 10), day(100, 3))
        val info = StreakCalculator.calculate(days, today = 100)
        assertEquals(2, info.current)
    }

    @Test
    fun streakBreaksAfterAMissedDay() {
        val days = listOf(day(95, 10), day(96, 10), day(97, 10), day(99, 10))
        val info = StreakCalculator.calculate(days, today = 100)
        assertEquals(1, info.current)
        assertEquals(3, info.longest)
    }

    @Test
    fun eachDayUsesTheGoalItWasRecordedWith() {
        val days = listOf(day(99, 10, goal = 10), day(100, 10, goal = 20))
        assertEquals(StreakInfo(current = 1, longest = 1), StreakCalculator.calculate(days, today = 100))
    }
}
