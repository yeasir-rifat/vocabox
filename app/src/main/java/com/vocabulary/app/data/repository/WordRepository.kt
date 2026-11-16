package com.vocabulary.app.data.repository

import com.vocabulary.app.data.local.dao.BookmarkDao
import com.vocabulary.app.data.local.dao.MistakeDao
import com.vocabulary.app.data.local.dao.WordDao
import com.vocabulary.app.data.local.dao.WordReadDao
import com.vocabulary.app.data.local.entity.WordReadEntity
import com.vocabulary.app.data.local.entity.BookmarkEntity
import com.vocabulary.app.data.local.entity.MistakeEntity
import com.vocabulary.app.data.local.entity.MistakeWithWord
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao,
    private val bookmarkDao: BookmarkDao,
    private val mistakeDao: MistakeDao,
    private val wordReadDao: WordReadDao
) {
    /** Marks a word as read (detail page opened or flashcard shown). Idempotent. */
    suspend fun markRead(wordId: Long, now: Long = System.currentTimeMillis()) =
        wordReadDao.insert(WordReadEntity(wordId, now))

    fun observeReadIds(): Flow<List<Long>> = wordReadDao.observeReadIds()

    fun observeWordCount(): Flow<Int> = wordDao.observeCount()

    fun observeWords(
        query: String = "",
        topic: String? = null,
        level: Int? = null,
        type: WordType? = null
    ): Flow<List<WordEntity>> = wordDao.observeWords(escapeLike(query.trim()), topic, level, type)

    /** Makes %, _ and \\ in the search text match themselves instead of acting as wildcards. */
    private fun escapeLike(text: String): String =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    fun observeWord(id: Long): Flow<WordEntity?> = wordDao.observeById(id)

    fun observeTopics(): Flow<List<String>> = wordDao.observeTopics()

    fun observeBookmarkedWords(): Flow<List<WordEntity>> = bookmarkDao.observeBookmarkedWords()

    fun observeMistakeWords(): Flow<List<WordEntity>> = mistakeDao.observeMistakeWords()

    fun observeMistakes(): Flow<List<MistakeWithWord>> = mistakeDao.observeMistakeEntries()

    suspend fun removeMistake(wordId: Long) = mistakeDao.remove(wordId)

    /** Puts a dismissed mistake back (Undo), with the number of times it was missed. */
    suspend fun restoreMistake(wordId: Long, count: Int) =
        mistakeDao.upsert(MistakeEntity(wordId, System.currentTimeMillis(), count))

    fun observeBookmarkedIds(): Flow<List<Long>> = bookmarkDao.observeBookmarkedIds()

    fun observeIsBookmarked(wordId: Long): Flow<Boolean> = bookmarkDao.observeIsBookmarked(wordId)

    suspend fun toggleBookmark(wordId: Long) {
        if (bookmarkDao.isBookmarked(wordId)) {
            bookmarkDao.delete(wordId)
        } else {
            bookmarkDao.insert(BookmarkEntity(wordId, System.currentTimeMillis()))
        }
    }
}
