package com.vocabulary.app.data.seed

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.domain.ItemQuestion
import com.vocabulary.app.domain.ItemQuestions
import kotlinx.serialization.Serializable

/** Root object of assets/words.json. Bump [version] whenever the word list changes. */
@Serializable
data class WordSeedFile(
    val version: Int,
    val words: List<WordSeedDto>
)

@Serializable
data class WordSeedDto(
    val id: Long,
    val word: String,
    val partOfSpeech: String,
    val banglaMeaning: String,
    val englishMeaning: String,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val example: String = "",
    val topic: String = "General",
    val level: Int = 1,
    val type: WordType = WordType.WORD,
    val synonymsBn: List<String> = emptyList(),
    val antonymsBn: List<String> = emptyList(),
    /** Optional. 1 moves this entry into the "Appendix" card of its category (exceptions). */
    val appendix: Int = 0,
    /** Optional. Question sentences / options for this item, see [ItemQuestion]. */
    val questions: List<ItemQuestion> = emptyList()
) {
    fun toEntity() = WordEntity(
        id = id,
        word = word.trim(),
        partOfSpeech = partOfSpeech.trim(),
        banglaMeaning = banglaMeaning.trim(),
        englishMeaning = englishMeaning.trim(),
        synonyms = cleanTerms(synonyms, synonymsBn).first,
        antonyms = cleanTerms(antonyms, antonymsBn).first,
        example = example.trim(),
        topic = topic.trim(),
        level = level.coerceIn(1, 3),
        type = type,
        synonymsBn = cleanTerms(synonyms, synonymsBn).second,
        antonymsBn = cleanTerms(antonyms, antonymsBn).second,
        appendix = appendix.coerceAtLeast(0),
        questions = ItemQuestions.encode(questions.filter { it.type.isNotBlank() })
    )
}

/**
 * Trims the terms, drops blank ones and keeps the Bangla list aligned with them
 * (a missing meaning becomes an empty string).
 */
private fun cleanTerms(terms: List<String>, bangla: List<String>): Pair<List<String>, List<String>> {
    val kept = terms.mapIndexedNotNull { i, term ->
        term.trim().takeIf { it.isNotEmpty() }?.let { it to bangla.getOrNull(i).orEmpty().trim() }
    }
    return kept.map { it.first } to kept.map { it.second }
}
