package com.vocabulary.app.domain

import java.util.Calendar
import java.util.TimeZone

/** Calendar-day helpers that work on API 24 without java.time. */
object DayUtils {

    const val DAY_MS = 86_400_000L

    /** Days since 1970-01-01 in the given time zone (device zone by default). */
    fun epochDay(timeMs: Long, tz: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(timeMs + tz.getOffset(timeMs), DAY_MS)

    /** Epoch day of a local date. [month0] is zero based, like java.util.Calendar. */
    fun epochDayOf(year: Int, month0: Int, dayOfMonth: Int): Long {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month0, dayOfMonth, 12, 0, 0)
        }
        return epochDay(cal.timeInMillis)
    }

    /** A moment around local noon of [epochDay], safe to format as a date. */
    fun noonMillis(epochDay: Long, tz: TimeZone = TimeZone.getDefault()): Long =
        (epochDay * DAY_MS + DAY_MS / 2).let { guess -> guess - tz.getOffset(guess) }
}
