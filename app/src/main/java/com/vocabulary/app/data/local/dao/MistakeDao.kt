package com.vocabulary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.vocabulary.app.data.local.entity.MistakeEntity
import com.vocabulary.app.data.local.entity.MistakeWithWord
import com.vocabulary.app.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MistakeDao {

    @Query("SELECT * FROM mistakes WHERE wordId = :wordId")
    abstract suspend fun get(wordId: Long): MistakeEntity?

    @Upsert
    abstract suspend fun upsert(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE wordId = :wordId")
    abstract suspend fun remove(wordId: Long)

    @Query("DELETE FROM mistakes")
    abstract suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM mistakes")
    abstract fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN mistakes m ON m.wordId = w.id
        ORDER BY m.lastWrongAt DESC
        """
    )
    abstract fun observeMistakeWords(): Flow<List<WordEntity>>

    @Query(
        """
        SELECT w.*, m.count AS count FROM words w
        INNER JOIN mistakes m ON m.wordId = w.id
        ORDER BY m.lastWrongAt DESC
        """
    )
    abstract fun observeMistakeEntries(): Flow<List<MistakeWithWord>>

    /** Most frequently missed words first. */
    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN mistakes m ON m.wordId = w.id
        ORDER BY m.count DESC, m.lastWrongAt DESC
        LIMIT :limit
        """
    )
    abstract suspend fun getMistakeWords(limit: Int): List<WordEntity>

    /** Create the mistake row or increase its counter. */
    @Transaction
    open suspend fun recordMistake(wordId: Long, now: Long) {
        val existing = get(wordId)
        upsert(
            MistakeEntity(
                wordId = wordId,
                lastWrongAt = now,
                count = (existing?.count ?: 0) + 1
            )
        )
    }
}
