package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.random.Random

class PrepositionFairnessTest {

    private fun prep(id: Long, word: String, example: String) = WordEntity(
        id = id,
        word = word,
        partOfSpeech = "preposition",
        banglaMeaning = "bn$id",
        englishMeaning = "eng$id",
        synonyms = emptyList(),
        antonyms = emptyList(),
        example = example,
        topic = "Prepositions",
        level = 1,
        type = WordType.PREPOSITION
    )

    @Test
    fun interchangeablePrepositionsAreNeverOfferedNextToTheAnswer() {
        val entry = prep(1, "depend on", "You can depend on him.")
        repeat(50) { seed ->
            val q = QuizGenerator.generate(QuizMode.PREPOSITION, listOf(entry), 1, Random(seed)).single()
            assertFalse("over/upon must not appear next to 'on'", "over" in q.options || "upon" in q.options)
        }
    }
}
