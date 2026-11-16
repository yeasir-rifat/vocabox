package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordType

/** Small pure helpers for the Saved tab, kept free of Android so they can be unit tested. */
object SavedSummary {

    /** Entries per type in [WordType] order. Types with no entry are left out. */
    fun typeCounts(words: List<WordEntity>): List<Pair<WordType, Int>> =
        WordType.entries.mapNotNull { type ->
            val count = words.count { it.type == type }
            if (count > 0) type to count else null
        }
}
