package com.vocabulary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vocabulary.app.data.local.entity.QuizResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizResultDao {

    @Insert
    suspend fun insert(result: QuizResultEntity): Long

    @Query("SELECT * FROM quiz_results ORDER BY takenAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<QuizResultEntity>>

    @Query("SELECT * FROM quiz_results WHERE takenAt >= :since ORDER BY takenAt ASC")
    fun observeSince(since: Long): Flow<List<QuizResultEntity>>

    @Query("DELETE FROM quiz_results")
    suspend fun clearAll()
}
