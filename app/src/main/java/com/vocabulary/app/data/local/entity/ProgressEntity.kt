package com.vocabulary.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vocabulary.app.data.model.WordStatus

/** Spaced-repetition state for one word. */
@Entity(
    tableName = "progress",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("nextReviewAt")]
)
data class ProgressEntity(
    @PrimaryKey val wordId: Long,
    val status: WordStatus = WordStatus.NEW,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val nextReviewAt: Long = 0L,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastReviewedAt: Long = 0L
)
