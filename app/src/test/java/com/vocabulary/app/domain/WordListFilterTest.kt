package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.ListOptions
import com.vocabulary.app.data.model.ReadFilter
import com.vocabulary.app.data.model.WordOrder
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordListFilterTest {

    private fun word(id: Long, text: String, type: WordType = WordType.WORD) = WordEntity(
        id = id,
        word = text,
        partOfSpeech = "noun",
        banglaMeaning = "",
        englishMeaning = "",
        synonyms = emptyList(),
        antonyms = emptyList(),
        example = "",
        topic = "General",
        level = 1,
        type = type
    )

    private val words = listOf(
        word(1, "Zeal"),
        word(2, "abate"),
        word(3, "Bold"),
        word(4, "candid"),
        word(5, "Brisk"),
        word(6, "\"Beat about the bush\"", WordType.IDIOM)
    )

    private fun apply(
        options: ListOptions = ListOptions(),
        read: Set<Long> = emptySet(),
        favourites: Set<Long> = emptySet()
    ) = WordListFilter.apply(words, read, favourites, options)

    private fun List<WordEntity>.ids() = map { it.id }

    @Test
    fun defaultsShowEveryWordInAlphabeticalOrderIgnoringCase() {
        // A quote mark sorts before letters, so the idiom comes first.
        assertEquals(listOf(6L, 2L, 3L, 5L, 4L, 1L), apply().ids())
    }

    @Test
    fun readAndUnreadSplitTheListByReadIds() {
        val read = setOf(1L, 3L)
        assertEquals(setOf(1L, 3L), apply(ListOptions(read = ReadFilter.READ), read = read).ids().toSet())
        assertEquals(
            setOf(2L, 4L, 5L, 6L),
            apply(ListOptions(read = ReadFilter.UNREAD), read = read).ids().toSet()
        )
    }

    @Test
    fun favouritesOnlyKeepsBookmarkedWords() {
        val result = apply(ListOptions(favouriteOnly = true), favourites = setOf(2L, 4L))
        assertEquals(setOf(2L, 4L), result.ids().toSet())
    }

    @Test
    fun readAndFavouriteFiltersMustBothMatch() {
        val result = apply(
            ListOptions(read = ReadFilter.UNREAD, favouriteOnly = true),
            read = setOf(2L),
            favourites = setOf(2L, 4L)
        )
        assertEquals(listOf(4L), result.ids())
    }

    @Test
    fun lettersKeepWordsStartingWithAnyChosenLetter() {
        val result = apply(ListOptions(letters = setOf('B', 'C')))
        // The idiom starts with B once the leading quote is skipped.
        assertEquals(setOf(3L, 4L, 5L, 6L), result.ids().toSet())
    }

    @Test
    fun initialSkipsLeadingQuotesAndIsUppercase() {
        assertEquals('B', words[5].initial())
        assertEquals('A', words[1].initial())
        assertEquals('#', word(9, "123").initial())
    }

    @Test
    fun countKeepsTheFirstWordsAfterFilteringAndOrdering() {
        val result = apply(ListOptions(letters = setOf('B', 'C'), count = 2))
        assertEquals(2, result.size)
        // A-Z inside B and C: the quoted idiom, then Bold.
        assertEquals(listOf(6L, 3L), result.ids())
    }

    @Test
    fun countLargerThanTheListKeepsEverything() {
        assertEquals(words.size, apply(ListOptions(count = 500)).size)
    }

    @Test
    fun randomOrderKeepsTheSameWordsAndRepeatsForTheSameSeed() {
        val options = ListOptions(order = WordOrder.RANDOM, shuffleSeed = 42L)
        val first = apply(options)
        val second = apply(options)
        assertEquals(first.ids(), second.ids())
        assertEquals(words.ids().toSet(), first.ids().toSet())
    }

    @Test
    fun randomOrderWithCountTakesFromTheFilteredWordsOnly() {
        val options = ListOptions(
            read = ReadFilter.UNREAD,
            order = WordOrder.RANDOM,
            shuffleSeed = 7L,
            count = 2
        )
        val result = apply(options, read = setOf(1L, 2L))
        assertEquals(2, result.size)
        assertTrue(result.ids().none { it == 1L || it == 2L })
    }

    @Test
    fun resettingOptionsIsNotActive() {
        assertFalse(ListOptions(shuffleSeed = 99L).isActive)
        assertTrue(ListOptions(order = WordOrder.RANDOM).isActive)
        assertTrue(ListOptions(count = 10).isActive)
    }

    @Test
    fun typeAndLevelNarrowTheList() {
        val mixed = words.map { if (it.id == 3L) it.copy(level = 2) else it }
        val idioms = WordListFilter.apply(mixed, emptySet(), emptySet(), ListOptions(type = WordType.IDIOM))
        assertEquals(listOf(6L), idioms.ids())
        val levelTwo = WordListFilter.apply(mixed, emptySet(), emptySet(), ListOptions(level = 2))
        assertEquals(listOf(3L), levelTwo.ids())
        assertTrue(ListOptions(type = WordType.IDIOM).isActive)
        assertTrue(ListOptions(level = 1).isActive)
    }
}
