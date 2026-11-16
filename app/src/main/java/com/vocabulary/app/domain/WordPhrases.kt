package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType

/**
 * Splits a preposition entry into the word and its preposition: "afraid of" becomes ("afraid", "of").
 * Returns null when the entry does not follow that pattern (a single word) or when it is an
 * appendix entry, which is an exception to the pattern and is shown whole.
 */
fun WordEntity.prepositionSplit(): Pair<String, String>? {
    if (type != WordType.PREPOSITION || appendix > 0) return null
    val parts = word.trim().split(Regex("\\s+"))
    if (parts.size < 2) return null
    return parts.dropLast(1).joinToString(" ") to parts.last()
}

/**
 * Other group verbs that start with the same verb, for example "look up" and "look into" for
 * "look after". Sorted alphabetically. Empty for every other type.
 */
fun WordEntity.relatedGroupVerbs(all: List<WordEntity>): List<WordEntity> {
    if (type != WordType.GROUP_VERB) return emptyList()
    val head = word.firstWord()
    if (head.isEmpty()) return emptyList()
    return all
        .filter { it.id != id && it.type == WordType.GROUP_VERB && it.word.firstWord() == head }
        .sortedWith(compareBy<WordEntity>({ it.word.lowercase() }, { it.id }))
}

private fun String.firstWord(): String = trim().substringBefore(' ').lowercase()
