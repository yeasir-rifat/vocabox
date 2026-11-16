package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.ListOptions
import com.vocabulary.app.data.model.ReadFilter
import com.vocabulary.app.data.model.WordOrder
import kotlin.random.Random

/** First letter of the word as written, uppercase. '#' when the entry has no letter at all. */
fun WordEntity.initial(): Char =
    word.trim().firstOrNull { it.isLetter() }?.uppercaseChar() ?: '#'

/** Applies [ListOptions] to a list of words: filters first, then the order, then the count. */
object WordListFilter {

    fun apply(
        words: List<WordEntity>,
        readIds: Set<Long>,
        favouriteIds: Set<Long>,
        options: ListOptions
    ): List<WordEntity> {
        val kept = words.filter { word ->
            val readOk = when (options.read) {
                ReadFilter.ALL -> true
                ReadFilter.READ -> word.id in readIds
                ReadFilter.UNREAD -> word.id !in readIds
            }
            readOk &&
                (!options.favouriteOnly || word.id in favouriteIds) &&
                (options.type == null || word.type == options.type) &&
                (options.level == null || word.level == options.level) &&
                (options.letters.isEmpty() || word.initial() in options.letters)
        }
        val ordered = when (options.order) {
            WordOrder.A_TO_Z -> kept.sortedBy { it.word.lowercase() }
            WordOrder.RANDOM -> kept.shuffled(Random(options.shuffleSeed))
        }
        val limit = options.count
        return if (limit != null && limit > 0) ordered.take(limit) else ordered
    }
}
