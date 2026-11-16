package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordPhrasesTest {

    private fun entry(id: Long, text: String, type: WordType, appendix: Int = 0) = WordEntity(
        id = id, word = text, partOfSpeech = "x", banglaMeaning = "bn", englishMeaning = "en",
        synonyms = emptyList(), antonyms = emptyList(), example = "", topic = "t", level = 1,
        type = type, appendix = appendix
    )

    @Test
    fun prepositionIsSplitOffTheEnd() {
        assertEquals("afraid" to "of", entry(1, "afraid of", WordType.PREPOSITION).prepositionSplit())
        assertEquals("in spite" to "of", entry(2, " in spite  of ", WordType.PREPOSITION).prepositionSplit())
    }

    @Test
    fun exceptionsAndOtherTypesAreNotSplit() {
        assertNull(entry(1, "afraid of", WordType.PREPOSITION, appendix = 1).prepositionSplit())
        assertNull(entry(2, "afraid", WordType.PREPOSITION).prepositionSplit())
        assertNull(entry(3, "look after", WordType.GROUP_VERB).prepositionSplit())
    }

    @Test
    fun relatedVerbsShareTheFirstWord() {
        val all = listOf(
            entry(1, "look after", WordType.GROUP_VERB),
            entry(2, "Look up", WordType.GROUP_VERB),
            entry(3, "look into", WordType.GROUP_VERB),
            entry(4, "give up", WordType.GROUP_VERB),
            entry(5, "look at", WordType.PREPOSITION)
        )
        assertEquals(listOf(3L, 2L), all[0].relatedGroupVerbs(all).map { it.id })
        assertEquals(emptyList<Long>(), all[3].relatedGroupVerbs(all).map { it.id })
        assertEquals(emptyList<Long>(), all[4].relatedGroupVerbs(all).map { it.id })
    }
}
