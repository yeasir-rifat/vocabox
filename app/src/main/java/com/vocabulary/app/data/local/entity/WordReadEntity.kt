package com.vocabulary.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** A word the user has read: opened its detail page or seen it as a flashcard. */
@Entity(
    tableName = "word_reads",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WordReadEntity(
    @PrimaryKey val wordId: Long,
    val firstReadAt: Long
)
