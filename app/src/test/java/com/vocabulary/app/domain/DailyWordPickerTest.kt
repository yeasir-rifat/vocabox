package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.ProgressEntity
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordStatus
import com.vocabulary.app.data.model.WordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyWordPickerTest {

    private fun word(id: Long) = WordEntity(
        id = id, word = "w$id", partOfSpeech = "noun", banglaMeaning = "b", englishMeaning = "e",
        synonyms = emptyList(), antonyms = emptyList(), example = "", topic = "t", level = 1,
        type = WordType.WORD
    )

    private val words = (1L..5L).map { word(it) }

    @Test
    fun sameDayGivesSameWord() {
        assertEquals(
            DailyWordPicker.pick(words, emptyMap(), 20_000),
            DailyWordPicker.pick(words.shuffled(), emptyMap(), 20_000)
        )
    }

    @Test
    fun differentDaysCycleThroughWords() {
        val picked = (0L..4L).map { DailyWordPicker.pick(words, emptyMap(), 20_000 + it)!!.id }.toSet()
        assertEquals(5, picked.size)
    }

    @Test
    fun masteredWordIsSkippedForTheLeastSeenOne() {
        val byDate = DailyWordPicker.pick(words, emptyMap(), 20_000)!!
        val progress = words.associate { it.id to ProgressEntity(it.id, WordStatus.LEARNING, correctCount = 5) }
            .toMutableMap()
        progress[byDate.id] = ProgressEntity(byDate.id, WordStatus.MASTERED, correctCount = 9)
        val least = words.first { it.id != byDate.id }
        progress[least.id] = ProgressEntity(least.id, WordStatus.LEARNING, correctCount = 1)

        assertEquals(least.id, DailyWordPicker.pick(words, progress, 20_000)!!.id)
    }

    @Test
    fun emptyListGivesNull() {
        assertNull(DailyWordPicker.pick(emptyList(), emptyMap(), 1))
    }
}
