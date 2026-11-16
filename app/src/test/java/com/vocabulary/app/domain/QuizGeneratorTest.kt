package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizGeneratorTest {

    private fun word(
        id: Long,
        type: WordType = WordType.WORD,
        synonyms: List<String> = listOf("syn${id}a", "syn${id}b"),
        antonyms: List<String> = listOf("ant${id}a")
    ) = WordEntity(
        id = id,
        word = "word$id",
        partOfSpeech = "adjective",
        banglaMeaning = "bn$id",
        englishMeaning = "eng$id",
        synonyms = synonyms,
        antonyms = antonyms,
        example = "Example $id.",
        topic = "topic",
        level = 1,
        type = type
    )

    private val words = (1L..8L).map { word(it) }

    @Test
    fun meaningQuestionsHaveFourDistinctOptionsAndTheRightAnswer() {
        val questions = QuizGenerator.generate(QuizMode.MEANING, words, 5, Random(1))
        assertEquals(5, questions.size)
        questions.forEach { q ->
            assertEquals(QuizGenerator.OPTION_COUNT, q.options.size)
            assertEquals(q.options.size, q.options.toSet().size)
            assertEquals("bn${q.wordId}", q.correctAnswer)
        }
    }

    @Test
    fun noQuestionsWhenThereAreTooFewWords() {
        val few = words.take(3)
        assertTrue(QuizGenerator.generate(QuizMode.MEANING, few, 5, Random(1)).isEmpty())
    }

    @Test
    fun synonymQuestionNeverOffersAnotherSynonymOfTheSameWord() {
        QuizGenerator.generate(QuizMode.SYNONYM, words, 8, Random(2)).forEach { q ->
            val source = words.first { it.id == q.wordId }
            assertTrue(q.correctAnswer in source.synonyms)
            q.options.filterIndexed { i, _ -> i != q.correctIndex }.forEach { wrong ->
                assertFalse(wrong in source.synonyms)
            }
        }
    }

    @Test
    fun antonymQuestionUsesAntonymAsCorrectAnswer() {
        QuizGenerator.generate(QuizMode.ANTONYM, words, 8, Random(3)).forEach { q ->
            val source = words.first { it.id == q.wordId }
            assertTrue(q.correctAnswer in source.antonyms)
        }
    }

    @Test
    fun availabilityIsZeroForModesWithoutEnoughContent() {
        val availability = QuizGenerator.availability(words)
        assertEquals(8, availability[QuizMode.MEANING])
        assertEquals(0, availability[QuizMode.IDIOM])
        assertEquals(0, availability[QuizMode.ONE_WORD_SUB])
    }

    @Test
    fun idiomQuestionsUseIdiomMeanings() {
        val idioms = (101L..105L).map { word(it, type = WordType.IDIOM, synonyms = emptyList(), antonyms = emptyList()) }
        val questions = QuizGenerator.generate(QuizMode.IDIOM, idioms, 3, Random(4))
        assertEquals(3, questions.size)
        questions.forEach { assertEquals("eng${it.wordId}", it.correctAnswer) }
    }

    @Test
    fun groupVerbAndProverbQuestionsUseTheirOwnMeanings() {
        val groupVerbs = (301L..305L).map { word(it, type = WordType.GROUP_VERB, synonyms = emptyList(), antonyms = emptyList()) }
        val proverbs = (501L..505L).map { word(it, type = WordType.PROVERB, synonyms = emptyList(), antonyms = emptyList()) }
        QuizGenerator.generate(QuizMode.GROUP_VERB, groupVerbs + proverbs, 3, Random(5)).forEach { q ->
            assertTrue(q.wordId in 301L..305L)
            assertEquals("eng${q.wordId}", q.correctAnswer)
        }
        QuizGenerator.generate(QuizMode.PROVERB, groupVerbs + proverbs, 3, Random(6)).forEach { q ->
            assertTrue(q.wordId in 501L..505L)
            assertEquals("eng${q.wordId}", q.correctAnswer)
        }
    }

    @Test
    fun prepositionQuestionBlanksOutThePreposition() {
        val entry = word(401, type = WordType.PREPOSITION, synonyms = emptyList(), antonyms = emptyList())
            .copy(word = "abide by", example = "Every citizen must Abide by the law.")
        val questions = QuizGenerator.generate(QuizMode.PREPOSITION, listOf(entry), 1, Random(7))
        assertEquals(1, questions.size)
        val q = questions.single()
        assertTrue(q.prompt.endsWith("Every citizen must Abide ____ the law."))
        assertEquals("by", q.correctAnswer)
        assertEquals(QuizGenerator.OPTION_COUNT, q.options.toSet().size)
    }

    @Test
    fun prepositionEntryWithoutPhraseInExampleIsSkipped() {
        val entry = word(402, type = WordType.PREPOSITION, synonyms = emptyList(), antonyms = emptyList())
            .copy(word = "depend on", example = "Example without the phrase.")
        assertTrue(QuizGenerator.generate(QuizMode.PREPOSITION, listOf(entry), 1, Random(8)).isEmpty())
        assertEquals(0, QuizGenerator.availability(listOf(entry))[QuizMode.PREPOSITION])
    }
}
