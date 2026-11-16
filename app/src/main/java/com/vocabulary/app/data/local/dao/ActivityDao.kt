package com.vocabulary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vocabulary.app.data.local.entity.DailyActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ActivityDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun ensureRow(row: DailyActivityEntity)

    @Query(
        "UPDATE daily_activity SET reviewed = reviewed + 1, correct = correct + :correct, goal = :goal WHERE day = :day"
    )
    abstract suspend fun bump(day: Long, correct: Int, goal: Int)

    /** Counts one answered card for [day]. */
    @Transaction
    open suspend fun record(day: Long, correct: Boolean, goal: Int) {
        ensureRow(DailyActivityEntity(day = day, reviewed = 0, correct = 0, goal = goal))
        bump(day, if (correct) 1 else 0, goal)
    }

    @Query("SELECT * FROM daily_activity ORDER BY day ASC")
    abstract fun observeAll(): Flow<List<DailyActivityEntity>>

    @Query("DELETE FROM daily_activity")
    abstract suspend fun clearAll()
}
