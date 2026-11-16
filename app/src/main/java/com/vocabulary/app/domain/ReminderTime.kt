package com.vocabulary.app.domain

import java.util.Calendar
import java.util.TimeZone

object ReminderTime {

    /** Milliseconds from [now] until the next local [hour]:[minute] (today if still ahead, else tomorrow). */
    fun millisUntilNext(
        now: Long,
        hour: Int,
        minute: Int,
        tz: TimeZone = TimeZone.getDefault()
    ): Long {
        val next = Calendar.getInstance(tz).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (next.timeInMillis <= now) next.add(Calendar.DAY_OF_MONTH, 1)
        return next.timeInMillis - now
    }
}
