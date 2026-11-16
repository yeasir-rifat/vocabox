package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ItemQuestionsTest {

    private fun word(id: Long, questions: String = "[]") = WordEntity(
        id = id, word = "word$id", partOfSpeech = "noun", banglaMeaning = "bn$id",
        englishMeaning = "eng$id", synonyms = listOf("syn$id"), antonyms = listOf("ant$id"),
        example = "Example $id.", topic = "t", level = 1, type = WordType.WORD, questions = questions
    )

    private val pool = (1L..12L).map { word(it) }

    @Test
    fun automaticQuestionIsAFullSentenceWithoutInstruction() {
        val q = QuizGenerator.generate(QuizMode.SYNONYM, pool, 1, Random(1)).single()
        assertEquals("", q.instruction)
        assertTrue(q.prompt.contains("\u2018"))
        assertTrue(q.prompt.contains("word"))
    }

    @Test
    fun itemSentenceAndOptionsAreUsed() {
        val raw = """[{"type":"SYNONYM","text":"Pick the twin of {word}","options":["a","b","c","d"],"answer":"c"}]"""
        val target = word(99, raw)
        val q = QuizGenerator.generate(QuizMode.SYNONYM, listOf(target), 1, Random(2), pool + target).single()
        assertEquals("Pick the twin of word99", q.prompt)
        assertEquals(setOf("a", "b", "c", "d"), q.options.toSet())
        assertEquals("c", q.correctAnswer)
    }

    @Test
    fun badOptionsFallBackToAutomaticOnes() {
        val raw = """[{"type":"SYNONYM","text":"Custom?","options":["a","b"],"answer":"a"}]"""
        val target = word(99, raw)
        val q = QuizGenerator.generate(QuizMode.SYNONYM, listOf(target), 1, Random(3), pool + target).single()
        assertEquals("Custom?", q.prompt)
        assertEquals(QuizGenerator.OPTION_COUNT, q.options.size)
        assertEquals("syn99", q.correctAnswer)
    }

    @Test
    fun itemOnlyAppearsInTheTypesItLists() {
        val raw = """[{"type":"MEANING","text":"What is {word}?"}]"""
        val target = word(99, raw)
        assertTrue(ItemQuestions.allows(target, QuizMode.MEANING))
        assertFalse(ItemQuestions.allows(target, QuizMode.ANTONYM))
        assertTrue(ItemQuestions.allows(word(5), QuizMode.ANTONYM))
        assertTrue(QuizGenerator.generate(QuizMode.ANTONYM, listOf(target), 5, Random(4), pool + target).isEmpty())
    }
}
