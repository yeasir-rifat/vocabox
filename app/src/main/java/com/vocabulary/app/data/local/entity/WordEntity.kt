package com.vocabulary.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vocabulary.app.data.model.WordType

@Entity(
    tableName = "words",
    indices = [Index("word"), Index("topic"), Index("level"), Index("type")]
)
data class WordEntity(
    @PrimaryKey val id: Long,
    val word: String,
    val partOfSpeech: String,
    val banglaMeaning: String,
    val englishMeaning: String,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val example: String,
    val topic: String,
    val level: Int,
    val type: WordType,
    /** Bangla meaning of each synonym, same order as [synonyms]. May be shorter or empty. */
    val synonymsBn: List<String> = emptyList(),
    /** Bangla meaning of each antonym, same order as [antonyms]. May be shorter or empty. */
    val antonymsBn: List<String> = emptyList(),
    /** 0 for a normal entry. Any value above 0 puts the entry into the "Appendix" card of its category. */
    val appendix: Int = 0,
    /** JSON list of [com.vocabulary.app.domain.ItemQuestion]. "[]" = automatic questions. */
    val questions: String = "[]"
)
