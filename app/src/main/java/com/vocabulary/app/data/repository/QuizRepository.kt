package com.vocabulary.app.data.repository

import com.vocabulary.app.data.local.dao.MistakeDao
import com.vocabulary.app.data.local.dao.QuizResultDao
import com.vocabulary.app.data.local.dao.WordDao
import com.vocabulary.app.data.local.entity.QuizResultEntity
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    private val wordDao: WordDao,
    private val mistakeDao: MistakeDao,
    private val quizResultDao: QuizResultDao,
    private val studyRepository: StudyRepository
) {
    suspend fun loadWords(): List<WordEntity> = wordDao.getAll()

    /**
     * Saves one answer: updates the spaced-repetition schedule, adds the word to the
     * mistakes list when wrong and removes it from the list when right.
     */
    suspend fun recordAnswer(wordId: Long, correct: Boolean, now: Long = System.currentTimeMillis()) {
        studyRepository.grade(wordId, correct, now)
        if (correct) mistakeDao.remove(wordId) else mistakeDao.recordMistake(wordId, now)
    }

    suspend fun saveResult(mode: QuizMode, total: Int, correct: Int, durationSec: Int, takenAt: Long) {
        quizResultDao.insert(
            QuizResultEntity(
                mode = mode,
                total = total,
                correct = correct,
                durationSec = durationSec,
                takenAt = takenAt
            )
        )
    }
}
