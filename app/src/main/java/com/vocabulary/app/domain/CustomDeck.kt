package com.vocabulary.app.domain

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hand-over point for flashcards: the Saved screen leaves the words picked in its settings panel
 * here, and the Flashcards screen takes them once when it opens. Null means "no custom words".
 */
@Singleton
class CustomDeck @Inject constructor() {
    private var ids: Set<Long>? = null

    fun set(wordIds: Set<Long>?) {
        ids = wordIds
    }

    /** Returns the words once and clears them, so later visits are not affected. */
    fun take(): Set<Long>? = ids.also { ids = null }
}
