package com.vocabulary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vocabulary.app.data.local.entity.WordReadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordReadDao {

    /** Keeps the first read time; reading a word again changes nothing. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(read: WordReadEntity)

    @Query("SELECT wordId FROM word_reads")
    fun observeReadIds(): Flow<List<Long>>

    @Query("DELETE FROM word_reads")
    suspend fun clearAll()
}
