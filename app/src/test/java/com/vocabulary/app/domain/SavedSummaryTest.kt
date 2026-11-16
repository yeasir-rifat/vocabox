package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSummaryTest {

    private fun word(id: Long, type: WordType) = WordEntity(
        id = id, word = "w$id", partOfSpeech = "noun", banglaMeaning = "bn", englishMeaning = "en",
        synonyms = emptyList(), antonyms = emptyList(), example = "", topic = "t", level = 1,
        type = type
    )

    @Test
    fun emptyListHasNoCounts() {
        assertTrue(SavedSummary.typeCounts(emptyList()).isEmpty())
    }

    @Test
    fun countsAreGroupedByTypeInEnumOrder() {
        val words = listOf(
            word(1, WordType.PROVERB),
            word(2, WordType.WORD),
            word(3, WordType.IDIOM),
            word(4, WordType.WORD),
            word(5, WordType.WORD)
        )
        assertEquals(
            listOf(WordType.WORD to 3, WordType.IDIOM to 1, WordType.PROVERB to 1),
            SavedSummary.typeCounts(words)
        )
    }
}
