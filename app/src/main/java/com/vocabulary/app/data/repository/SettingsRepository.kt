package com.vocabulary.app.data.repository

import androidx.room.withTransaction
import com.vocabulary.app.data.local.VocabularyDatabase
import com.vocabulary.app.data.local.dao.ActivityDao
import com.vocabulary.app.data.local.dao.MistakeDao
import com.vocabulary.app.data.local.dao.ProgressDao
import com.vocabulary.app.data.local.dao.QuizResultDao
import com.vocabulary.app.data.local.dao.WordReadDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val database: VocabularyDatabase,
    private val progressDao: ProgressDao,
    private val mistakeDao: MistakeDao,
    private val quizResultDao: QuizResultDao,
    private val activityDao: ActivityDao,
    private val wordReadDao: WordReadDao
) {

    /**
     * Clears review progress, mistakes, quiz history, streak activity and read marks in one transaction.
     * Words and bookmarks are kept.
     */
    suspend fun resetProgress() {
        database.withTransaction {
            progressDao.clearAll()
            mistakeDao.clearAll()
            quizResultDao.clearAll()
            activityDao.clearAll()
            wordReadDao.clearAll()
        }
    }
}
