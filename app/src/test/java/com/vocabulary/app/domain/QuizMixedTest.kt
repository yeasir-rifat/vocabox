package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizMixedTest {

    private fun entry(id: Long, type: WordType, word: String = "word$id", example: String = "Example $id.") =
        WordEntity(
            id = id,
            word = word,
            partOfSpeech = "noun",
            banglaMeaning = "bn$id",
            englishMeaning = "eng$id",
            synonyms = if (type == WordType.WORD) listOf("syn$id") else emptyList(),
            antonyms = if (type == WordType.WORD) listOf("ant$id") else emptyList(),
            example = example,
            topic = "t",
            level = 1,
            type = type
        )

    private val plain = (1L..10L).map { entry(it, WordType.WORD) }
    private val idioms = (101L..106L).map { entry(it, WordType.IDIOM) }
    private val proverbs = (201L..204L).map { entry(it, WordType.PROVERB) }
    private val all = plain + idioms + proverbs

    // ---- generateMixed ----

    @Test
    fun emptySelectionReturnsNothing() {
        assertTrue(QuizGenerator.generateMixed(emptySet(), all, 10, Random(1)).isEmpty())
        assertTrue(QuizGenerator.generateMixed(setOf(QuizMode.IDIOM), all, 0, Random(1)).isEmpty())
    }

    @Test
    fun questionsAreSharedEvenlyAcrossModes() {
        val modes = setOf(QuizMode.MEANING, QuizMode.IDIOM, QuizMode.PROVERB)
        val questions = QuizGenerator.generateMixed(modes, all, 9, Random(2))
        assertEquals(9, questions.size)
        val byMode = questions.groupingBy { it.mode }.eachCount()
        assertEquals(3, byMode[QuizMode.MEANING])
        assertEquals(3, byMode[QuizMode.IDIOM])
        assertEquals(3, byMode[QuizMode.PROVERB])
    }

    @Test
    fun aSmallModeIsExhaustedAndOthersFillTheRest() {
        // Proverbs: only 4 usable, so 10 questions = 4 proverbs + 6 idioms.
        val questions = QuizGenerator.generateMixed(setOf(QuizMode.IDIOM, QuizMode.PROVERB), all, 10, Random(3))
        assertEquals(10, questions.size)
        assertEquals(4, questions.count { it.mode == QuizMode.PROVERB })
        assertEquals(6, questions.count { it.mode == QuizMode.IDIOM })
    }

    @Test
    fun noWordIsAskedTwiceEvenWhenModesOverlap() {
        // MEANING, SYNONYM and ANTONYM all use the same 10 plain words.
        val modes = setOf(QuizMode.MEANING, QuizMode.SYNONYM, QuizMode.ANTONYM)
        val questions = QuizGenerator.generateMixed(modes, all, 30, Random(4))
        assertEquals(questions.size, questions.map { it.wordId }.toSet().size)
        assertTrue(questions.size <= 10)
    }

    @Test
    fun totalNeverExceedsRequest() {
        val questions = QuizGenerator.generateMixed(QuizMode.entries.toSet(), all, 5, Random(5))
        assertEquals(5, questions.size)
    }

    @Test
    fun distractorsComeFromTheSameType() {
        val questions = QuizGenerator.generateMixed(setOf(QuizMode.IDIOM, QuizMode.PROVERB), all, 10, Random(6))
        val idiomMeanings = idioms.map { it.englishMeaning }.toSet()
        val proverbMeanings = proverbs.map { it.englishMeaning }.toSet()
        questions.forEach { q ->
            val allowed = if (q.mode == QuizMode.IDIOM) idiomMeanings else proverbMeanings
            assertTrue(q.options.all { it in allowed })
        }
    }

    @Test
    fun prepositionDistractorsAreCommonPrepositions() {
        val prep = entry(301, WordType.PREPOSITION, word = "abide by", example = "You must abide by the rules.")
        val q = QuizGenerator.generateMixed(setOf(QuizMode.PREPOSITION), listOf(prep), 1, Random(7)).single()
        assertEquals("by", q.correctAnswer)
        assertEquals(setOf("of", "in", "on", "at", "for", "with", "by", "to", "from", "into", "over", "against")
            .containsAll(q.options.toSet()), true)
    }

    // ---- availability and sources ----

    @Test
    fun availabilityIsComputedPerSource() {
        val favorites = plain.take(2) + idioms.take(1)
        val favAvailability = QuizGenerator.availability(favorites, pool = all)
        assertEquals(2, favAvailability[QuizMode.MEANING])
        assertEquals(1, favAvailability[QuizMode.IDIOM])
        assertEquals(0, favAvailability[QuizMode.PROVERB])
    }

    @Test
    fun smallSourceFallsBackToTheFullPoolForDistractors() {
        val favorites = listOf(plain.first())
        // On its own one word cannot make four options.
        assertEquals(0, QuizGenerator.availability(favorites)[QuizMode.MEANING])
        // With the full pool it can.
        assertEquals(1, QuizGenerator.availability(favorites, pool = all)[QuizMode.MEANING])
        val q = QuizGenerator.generateMixed(setOf(QuizMode.MEANING), favorites, 5, Random(8), pool = all).single()
        assertEquals(plain.first().id, q.wordId)
        assertEquals(QuizGenerator.OPTION_COUNT, q.options.toSet().size)
    }

    @Test
    fun totalForCountsSharedWordsOnce() {
        val ids = QuizGenerator.buildableIds(all)
        val availability = QuizAvailability(ids, loaded = true)
        val sum = availability.count(QuizMode.MEANING) + availability.count(QuizMode.SYNONYM)
        assertEquals(20, sum)
        assertEquals(10, availability.totalFor(listOf(QuizMode.MEANING, QuizMode.SYNONYM)))
    }

    // ---- limits ----

    @Test
    fun countAndMinutesAreClamped() {
        assertEquals(1, QuizLimits.clampCount(0))
        assertEquals(1, QuizLimits.clampCount(-5))
        assertEquals(100, QuizLimits.clampCount(250))
        assertEquals(42, QuizLimits.clampCount(42))
        assertEquals(0, QuizLimits.clampMinutes(-1))
        assertEquals(180, QuizLimits.clampMinutes(999))
        assertEquals(0, QuizLimits.clampMinutes(0))
    }

    @Test
    fun effectiveCountIsCappedByAvailability() {
        assertEquals(7, QuizLimits.effectiveCount(10, 7))
        assertEquals(10, QuizLimits.effectiveCount(10, 50))
        assertEquals(0, QuizLimits.effectiveCount(10, 0))
    }

    // ---- scoring ----

    @Test
    fun listScoringCountsUnansweredAsWrong() {
        val questions = QuizGenerator.generateMixed(setOf(QuizMode.MEANING), all, 4, Random(9))
        assertEquals(4, questions.size)
        val answers = mapOf(
            0 to questions[0].correctIndex,
            1 to (questions[1].correctIndex + 1) % 4,
            2 to questions[2].correctIndex
            // question 3 left unanswered
        )
        assertEquals(2, QuizScoring.correctCount(questions, answers))
        assertEquals(0, QuizScoring.correctCount(questions, emptyMap()))
    }
}
