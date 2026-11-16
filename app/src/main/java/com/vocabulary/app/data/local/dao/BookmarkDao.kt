package com.vocabulary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vocabulary.app.data.local.entity.BookmarkEntity
import com.vocabulary.app.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE wordId = :wordId")
    suspend fun delete(wordId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE wordId = :wordId)")
    suspend fun isBookmarked(wordId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE wordId = :wordId)")
    fun observeIsBookmarked(wordId: Long): Flow<Boolean>

    @Query("SELECT wordId FROM bookmarks")
    fun observeBookmarkedIds(): Flow<List<Long>>

    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN bookmarks b ON b.wordId = w.id
        ORDER BY b.createdAt DESC
        """
    )
    fun observeBookmarkedWords(): Flow<List<WordEntity>>

    @Query(
        """
        SELECT w.* FROM words w
        INNER JOIN bookmarks b ON b.wordId = w.id
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getBookmarkedWords(limit: Int): List<WordEntity>
}
