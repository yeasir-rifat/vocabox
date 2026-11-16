package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.GroupOrder
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordGrouperTest {

    private fun word(id: Long, text: String) = WordEntity(
        id = id, word = text, partOfSpeech = "noun", banglaMeaning = "bn", englishMeaning = "en",
        synonyms = emptyList(), antonyms = emptyList(), example = "", topic = "t", level = 1,
        type = WordType.WORD
    )

    private val words = (1L..45L).map { word(it, "w%02d".format(46 - it)) }

    @Test
    fun groupsHaveTwentyWordsAndTheRestInTheLast() {
        val groups = WordGrouper.group(words, GroupOrder.ALPHABETICAL, 1)
        assertEquals(listOf(20, 20, 5), groups.map { it.words.size })
        assertEquals(listOf(1, 2, 3), groups.map { it.number })
    }

    @Test
    fun alphabeticalGroupsAreSortedAndHaveARange() {
        val groups = WordGrouper.group(words, GroupOrder.ALPHABETICAL, 1)
        val all = groups.flatMap { it.words.map { w -> w.word } }
        assertEquals(all.sorted(), all)
        assertEquals("W01 – W20", groups.first().range)
        assertEquals("Group 1 · W01 – W20", groups.first().title)
    }

    @Test
    fun shuffledGroupsCoverEveryWordOnceAndAreStableForOneSeed() {
        val a = WordGrouper.group(words.reversed(), GroupOrder.SHUFFLED, 7)
        val b = WordGrouper.group(words, GroupOrder.SHUFFLED, 7)
        assertEquals(a.map { g -> g.words.map { it.id } }, b.map { g -> g.words.map { it.id } })
        assertEquals(words.map { it.id }.toSet(), a.flatMap { it.words }.map { it.id }.toSet())
        assertEquals(45, a.sumOf { it.words.size })
        assertEquals("Group 1", a.first().title)
    }

    @Test
    fun anotherSeedGivesAnotherShuffle() {
        val a = WordGrouper.group(words, GroupOrder.SHUFFLED, 1).first().words.map { it.id }
        val b = WordGrouper.group(words, GroupOrder.SHUFFLED, 2).first().words.map { it.id }
        assertNotEquals(a, b)
        assertTrue(WordGrouper.group(emptyList(), GroupOrder.ALPHABETICAL, 1).isEmpty())
    }

    @Test
    fun appendixEntriesAreCollectedInOneCardAfterTheNormalGroups() {
        // Different appendix numbers still end up in the same single "Appendix" card.
        val mixed = words.map { w ->
            when (w.id) {
                1L, 2L -> w.copy(appendix = 1)
                3L -> w.copy(appendix = 2)
                else -> w
            }
        }
        val groups = WordGrouper.group(mixed, GroupOrder.ALPHABETICAL, 1)
        assertEquals(listOf(20, 20, 2, 3), groups.map { it.words.size })
        assertEquals("Appendix", groups.last().title)
        assertEquals(listOf(0, 1, 2, 3), groups.map { it.index })
        assertEquals(setOf(1L, 2L, 3L), groups.last().words.map { it.id }.toSet())
        assertEquals(mixed.size, groups.sumOf { it.words.size })
    }

    @Test
    fun noAppendixCardWithoutExceptions() {
        val groups = WordGrouper.group(words, GroupOrder.SHUFFLED, 1)
        assertTrue(groups.none { it.isAppendix })
    }
}
