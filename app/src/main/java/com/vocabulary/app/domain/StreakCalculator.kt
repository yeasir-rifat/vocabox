package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.DailyActivityEntity
import kotlin.math.max

data class StreakInfo(val current: Int = 0, val longest: Int = 0)

object StreakCalculator {

    /**
     * A day counts when its goal was met. The current streak ends today if today is done,
     * otherwise yesterday, so it is not lost until a whole day has passed without reaching the goal.
     */
    fun calculate(days: List<DailyActivityEntity>, today: Long): StreakInfo {
        val met = days.filter { it.goalMet }.map { it.day }.toSortedSet()
        if (met.isEmpty()) return StreakInfo()

        var cursor = if (today in met) today else today - 1
        var current = 0
        while (cursor in met) {
            current++
            cursor--
        }

        var longest = 0
        var run = 0
        var previous: Long? = null
        for (day in met) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            longest = max(longest, run)
            previous = day
        }
        return StreakInfo(current = current, longest = longest)
    }
}
