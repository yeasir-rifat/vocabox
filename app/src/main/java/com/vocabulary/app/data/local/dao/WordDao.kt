package com.vocabulary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    /** Insert new words and update existing ones without deleting rows (keeps user progress). */
    @Upsert
    suspend fun upsertAll(words: List<WordEntity>)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT id FROM words")
    suspend fun getAllIds(): List<Long>

    /** Removes words that left words.json. Progress, bookmarks and mistakes go with them (cascade). */
    @Query("DELETE FROM words WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM words")
    suspend fun getAll(): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM words
        WHERE (:query = '' OR word LIKE '%' || :query || '%' ESCAPE '\'
                OR banglaMeaning LIKE '%' || :query || '%' ESCAPE '\'
                OR englishMeaning LIKE '%' || :query || '%' ESCAPE '\'
                OR synonyms LIKE '%' || :query || '%' ESCAPE '\')
          AND (:topic IS NULL OR topic = :topic)
          AND (:level IS NULL OR level = :level)
          AND (:type IS NULL OR type = :type)
        ORDER BY word COLLATE NOCASE ASC
        """
    )
    fun observeWords(
        query: String,
        topic: String?,
        level: Int?,
        type: WordType?
    ): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE id = :id")
    fun observeById(id: Long): Flow<WordEntity?>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<WordEntity>

    @Query("SELECT DISTINCT topic FROM words ORDER BY topic COLLATE NOCASE ASC")
    fun observeTopics(): Flow<List<String>>

    @Query("SELECT * FROM words WHERE type = :type ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomByType(type: WordType, limit: Int): List<WordEntity>

    /** Words whose review date has arrived, most overdue first. */
    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN progress p ON p.wordId = w.id
        WHERE p.nextReviewAt <= :now
          AND (:topic IS NULL OR w.topic = :topic)
          AND (:level IS NULL OR w.level = :level)
          AND (:type IS NULL OR w.type = :type)
        ORDER BY p.nextReviewAt ASC
        LIMIT :limit
        """
    )
    suspend fun getDueWords(
        now: Long,
        topic: String?,
        level: Int?,
        type: WordType?,
        limit: Int
    ): List<WordEntity>

    /** Words the user has never reviewed (no progress row), easiest first. */
    @Query(
        """
        SELECT * FROM words
        WHERE id NOT IN (SELECT wordId FROM progress)
          AND (:topic IS NULL OR topic = :topic)
          AND (:level IS NULL OR level = :level)
          AND (:type IS NULL OR type = :type)
        ORDER BY level ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getNewWords(
        topic: String?,
        level: Int?,
        type: WordType?,
        limit: Int
    ): List<WordEntity>

    @Query(
        """
        SELECT COUNT(*) FROM words w
        INNER JOIN progress p ON p.wordId = w.id
        WHERE p.nextReviewAt <= :now
          AND (:topic IS NULL OR w.topic = :topic)
          AND (:level IS NULL OR w.level = :level)
          AND (:type IS NULL OR w.type = :type)
        """
    )
    fun observeDueWordCount(now: Long, topic: String?, level: Int?, type: WordType?): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM words
        WHERE id NOT IN (SELECT wordId FROM progress)
          AND (:topic IS NULL OR topic = :topic)
          AND (:level IS NULL OR level = :level)
          AND (:type IS NULL OR type = :type)
        """
    )
    fun observeNewWordCount(topic: String?, level: Int?, type: WordType?): Flow<Int>
}
