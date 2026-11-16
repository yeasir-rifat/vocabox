package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.ProgressEntity
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordStatus
import com.vocabulary.app.data.model.WordType

object DailyWordPicker {

    /**
     * Same word for the whole day (picked by date). If the user already mastered that word,
     * falls back to the least-seen word that is not mastered yet.
     */
    fun pick(words: List<WordEntity>, progress: Map<Long, ProgressEntity>, today: Long): WordEntity? {
        val sorted = words.filter { it.type == WordType.WORD }
            .ifEmpty { words }
            .sortedBy { it.id }
        if (sorted.isEmpty()) return null

        val n = sorted.size.toLong()
        val byDate = sorted[(((today % n) + n) % n).toInt()]
        if (progress[byDate.id]?.status != WordStatus.MASTERED) return byDate

        return sorted
            .filter { progress[it.id]?.status != WordStatus.MASTERED }
            .minWithOrNull(compareBy({ seen(progress[it.id]) }, { it.id }))
            ?: byDate
    }

    private fun seen(p: ProgressEntity?): Int = (p?.correctCount ?: 0) + (p?.wrongCount ?: 0)
}
