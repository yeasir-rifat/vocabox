package com.vocabulary.app.domain

import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.data.seed.WordSeedDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordMergeTest {

    private fun w(id: Long, word: String, bn: String = "অর্থ", type: WordType = WordType.WORD) = WordSeedDto(
        id = id, word = word, partOfSpeech = "noun", banglaMeaning = bn, englishMeaning = "meaning", type = type
    )

    @Test
    fun addsNewUpdatesChangedAndKeepsTheRest() {
        val existing = listOf(w(1, "a"), w(2, "b"), w(3, "c"))
        val plan = WordMerge.plan(existing, listOf(w(2, "b", bn = "নতুন"), w(3, "c"), w(4, "d")))

        assertTrue(plan.errors.isEmpty())
        assertEquals(1, plan.added)
        assertEquals(1, plan.updated)
        assertEquals(1, plan.unchanged)
        // Word 1 is not in the file but is kept.
        assertEquals(listOf(1L, 2L, 3L, 4L), plan.merged.map { it.id })
        assertEquals("নতুন", plan.merged.first { it.id == 2L }.banglaMeaning)
    }

    @Test
    fun sameWordUnderAnotherIdIsRejected() {
        val plan = WordMerge.plan(listOf(w(1, "Abandon")), listOf(w(9, " abandon ")))
        assertEquals(1, plan.errors.size)
        assertEquals(1, plan.merged.size)
    }

    @Test
    fun sameWordInAnotherCategoryIsAllowed() {
        val plan = WordMerge.plan(listOf(w(1, "break")), listOf(w(2, "break", type = WordType.IDIOM)))
        assertTrue(plan.errors.isEmpty())
        assertEquals(2, plan.merged.size)
    }

    @Test
    fun duplicateIdsAndEmptyFieldsAreReported() {
        val plan = WordMerge.plan(emptyList(), listOf(w(1, "a"), w(1, "b"), w(2, "c", bn = " ")))
        assertEquals(2, plan.errors.size)
    }
}
