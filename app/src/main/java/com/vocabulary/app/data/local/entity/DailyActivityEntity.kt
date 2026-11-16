package com.vocabulary.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Study activity of one local calendar day.
 * [day] is the epoch day (days since 1970-01-01 in the device time zone).
 * [goal] is the daily goal in force when the row was last updated, so changing the goal
 * later does not rewrite old streaks.
 */
@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val day: Long,
    val reviewed: Int,
    val correct: Int,
    val goal: Int
) {
    val goalMet: Boolean get() = goal > 0 && reviewed >= goal
}
