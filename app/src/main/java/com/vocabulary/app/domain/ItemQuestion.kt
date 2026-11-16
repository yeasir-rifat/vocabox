package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One question an item (word, idiom, proverb...) can be asked as. Lives in words.json under "questions".
 *
 * - [type] is a QuizMode name: MEANING, SYNONYM, ANTONYM, ONE_WORD_SUB, IDIOM, GROUP_VERB, PREPOSITION, PROVERB.
 * - [text] is the whole question sentence, always written in English. It may use {word}, {meaning} (Bangla), {english}, {example}
 *   and, for PREPOSITION, {blank} (the example with a gap). Blank = a built-in sentence is used.
 * - [options] + [answer] are optional. Options are English; a Bangla option is fine where the question asks for a Bangla meaning.
 *   With exactly four distinct options that contain the answer they are
 *   used as written; otherwise the wrong options are picked automatically.
 *
 * An item that lists questions is only asked in the types it lists. An item with no list keeps the
 * automatic behaviour.
 */
@Serializable
data class ItemQuestion(
    val type: String,
    val text: String = "",
    val options: List<String> = emptyList(),
    val answer: String = ""
)

object ItemQuestions {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val serializer = ListSerializer(ItemQuestion.serializer())

    fun parse(raw: String): List<ItemQuestion> {
        if (raw.length < 3) return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    fun encode(list: List<ItemQuestion>): String =
        if (list.isEmpty()) "[]" else json.encodeToString(serializer, list)

    fun forMode(word: WordEntity, mode: QuizMode): List<ItemQuestion> =
        parse(word.questions).filter { it.type.trim().equals(mode.name, ignoreCase = true) }

    /** True when the item lists no question types at all, or lists [mode]. */
    fun allows(word: WordEntity, mode: QuizMode): Boolean {
        val all = parse(word.questions)
        return all.isEmpty() || all.any { it.type.trim().equals(mode.name, ignoreCase = true) }
    }

    fun resolve(text: String, word: WordEntity, blank: String?): String =
        text.replace("{word}", word.word)
            .replace("{meaning}", word.banglaMeaning)
            .replace("{english}", word.englishMeaning)
            .replace("{example}", word.example)
            .replace("{blank}", blank.orEmpty())
}
