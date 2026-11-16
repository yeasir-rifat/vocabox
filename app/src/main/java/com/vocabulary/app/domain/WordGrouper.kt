package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.GroupOrder
import com.vocabulary.app.data.model.QuizSource
import kotlin.random.Random

/**
 * One study group of up to [WordGrouper.SIZE] words. [index] is zero based.
 * [isAppendix] marks the single "Appendix" card that holds the exceptions of a category.
 */
data class WordGroup(
    val index: Int,
    val words: List<WordEntity>,
    val alphabetical: Boolean,
    val isAppendix: Boolean = false
) {
    val number: Int get() = index + 1

    /** "Ad – Am" for alphabetical groups, empty for shuffled ones and the appendix. */
    val range: String
        get() = if (!isAppendix && alphabetical && words.isNotEmpty()) {
            "${words.first().word.prefixLabel()} – ${words.last().word.prefixLabel()}"
        } else {
            ""
        }

    val title: String
        get() = when {
            isAppendix -> "Appendix"
            range.isEmpty() -> "Group $number"
            else -> "Group $number · $range"
        }

    private fun String.prefixLabel(): String =
        trim().take(2).lowercase().replaceFirstChar { it.uppercase() }
}

/** Splits words into study groups. Pure Kotlin, so it can be unit tested. */
object WordGrouper {
    const val SIZE = 20

    /**
     * ALPHABETICAL sorts by word. SHUFFLED sorts by id first and then shuffles with [seed], so the
     * same seed always gives the same groups, whatever order the database returns the rows in.
     *
     * Entries marked as exceptions (appendix above 0) are kept out of the normal groups and
     * collected in one card named "Appendix", after all normal groups.
     */
    fun group(words: List<WordEntity>, order: GroupOrder, seed: Long): List<WordGroup> {
        val (exceptions, regular) = words.partition { it.appendix > 0 }
        val alphabetical = order == GroupOrder.ALPHABETICAL

        val groups = ordered(regular, order, seed).chunked(SIZE).mapIndexed { i, chunk ->
            WordGroup(i, chunk, alphabetical)
        }
        if (exceptions.isEmpty()) return groups
        return groups + WordGroup(groups.size, ordered(exceptions, order, seed), alphabetical, isAppendix = true)
    }

    private fun ordered(words: List<WordEntity>, order: GroupOrder, seed: Long): List<WordEntity> =
        when (order) {
            GroupOrder.ALPHABETICAL ->
                words.sortedWith(compareBy<WordEntity>({ it.word.lowercase() }, { it.id }))
            GroupOrder.SHUFFLED -> words.sortedBy { it.id }.shuffled(Random(seed))
        }
}

/**
 * Carries "quiz this" from a screen to the Quiz tab.
 *
 * A group sends its own [wordIds] with the default [source] GROUP. Favorites and Mistakes send an
 * empty id set and only name the [source], so the quiz always reads the live list.
 */
class QuizGroupRequest(
    val label: String,
    val wordIds: Set<Long>,
    val source: QuizSource = QuizSource.GROUP
)
