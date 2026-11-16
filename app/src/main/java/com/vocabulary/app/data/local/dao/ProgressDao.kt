package com.vocabulary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vocabulary.app.data.local.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("DELETE FROM progress")
    suspend fun clearAll()

    @Query("SELECT * FROM progress WHERE wordId = :wordId")
    suspend fun get(wordId: Long): ProgressEntity?

    @Query("SELECT * FROM progress")
    suspend fun getAll(): List<ProgressEntity>

    @Query("SELECT * FROM progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE wordId = :wordId")
    fun observe(wordId: Long): Flow<ProgressEntity?>

    @Query(
        """
        SELECT * FROM progress
        WHERE nextReviewAt <= :now
        ORDER BY nextReviewAt ASC
        LIMIT :limit
        """
    )
    suspend fun getDue(now: Long, limit: Int): List<ProgressEntity>

    @Query("SELECT COUNT(*) FROM progress WHERE nextReviewAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM progress WHERE status = 'MASTERED'")
    fun observeMasteredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM progress WHERE status = 'LEARNING'")
    fun observeLearningCount(): Flow<Int>
}
