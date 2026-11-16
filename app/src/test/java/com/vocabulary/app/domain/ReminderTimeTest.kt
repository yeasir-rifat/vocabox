package com.vocabulary.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderTimeTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val hour = 3_600_000L

    private fun at(hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 15, hour, minute, 0)
        }.timeInMillis

    @Test
    fun laterTodayWaitsUntilThen() {
        assertEquals(2 * hour, ReminderTime.millisUntilNext(at(6, 0), 8, 0, utc))
    }

    @Test
    fun alreadyPassedWaitsUntilTomorrow() {
        assertEquals(22 * hour + hour / 2, ReminderTime.millisUntilNext(at(9, 30), 8, 0, utc))
    }

    @Test
    fun exactlyNowWaitsAFullDay() {
        assertEquals(24 * hour, ReminderTime.millisUntilNext(at(8, 0), 8, 0, utc))
    }

    @Test
    fun minutesAreRespected() {
        assertEquals(45 * 60_000L, ReminderTime.millisUntilNext(at(20, 15), 21, 0, utc))
    }
}
