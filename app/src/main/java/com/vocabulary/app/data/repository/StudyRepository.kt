package com.vocabulary.app.data.repository

import com.vocabulary.app.data.local.dao.ActivityDao
import com.vocabulary.app.data.local.dao.BookmarkDao
import com.vocabulary.app.data.local.dao.MistakeDao
import com.vocabulary.app.data.local.dao.ProgressDao
import com.vocabulary.app.data.local.dao.WordDao
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.DeckSource
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.domain.DayUtils
import com.vocabulary.app.domain.SpacedRepetition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** What a flashcard session should contain. Filters apply to the SMART source only. */
data class DeckSpec(
    val source: DeckSource,
    val size: Int,
    val type: WordType? = null,
    val topic: String? = null,
    val level: Int? = null,
    /** When set, the deck is drawn from exactly these words (used by the Saved settings panel). */
    val wordIds: Set<Long>? = null
)

@Singleton
class StudyRepository @Inject constructor(
    private val wordDao: WordDao,
    private val progressDao: ProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val mistakeDao: MistakeDao,
    private val activityDao: ActivityDao,
    private val preferences: AppPreferences
) {

    /** Builds a shuffled deck. SMART = words due for review first, then new words. */
    suspend fun buildDeck(spec: DeckSpec, now: Long = System.currentTimeMillis()): List<WordEntity> {
        if (spec.wordIds != null) {
            val custom = spec.wordIds.toList().chunked(500).flatMap { wordDao.getByIds(it) }
            return custom.shuffled().take(spec.size)
        }
        val words = when (spec.source) {
            DeckSource.SMART -> {
                val due = wordDao.getDueWords(now, spec.topic, spec.level, spec.type, spec.size)
                val remaining = spec.size - due.size
                val fresh = if (remaining > 0) {
                    wordDao.getNewWords(spec.topic, spec.level, spec.type, remaining)
                } else {
                    emptyList()
                }
                due + fresh
            }
            DeckSource.BOOKMARKED -> bookmarkDao.getBookmarkedWords(spec.size)
            DeckSource.MISTAKES -> mistakeDao.getMistakeWords(spec.size)
        }
        return words.shuffled()
    }

    /** Saves the spaced-repetition result of one answer and counts it for today's goal. */
    suspend fun grade(wordId: Long, known: Boolean, now: Long = System.currentTimeMillis()) {
        val updated = SpacedRepetition.review(progressDao.get(wordId), wordId, known, now)
        progressDao.upsert(updated)
        activityDao.record(DayUtils.epochDay(now), known, preferences.dailyGoal.first())
    }

    fun observeDueCount(now: Long, topic: String?, level: Int?, type: WordType?): Flow<Int> =
        wordDao.observeDueWordCount(now, topic, level, type)

    fun observeNewCount(topic: String?, level: Int?, type: WordType?): Flow<Int> =
        wordDao.observeNewWordCount(topic, level, type)

    fun observeBookmarkCount(): Flow<Int> =
        bookmarkDao.observeBookmarkedIds().map { it.size }

    fun observeMistakeCount(): Flow<Int> = mistakeDao.observeCount()
}
