package com.vocabulary.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vocabulary.app.data.model.QuizMode

@Entity(tableName = "quiz_results", indices = [Index("takenAt")])
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: QuizMode,
    val total: Int,
    val correct: Int,
    val durationSec: Int,
    val takenAt: Long
)
