package com.vocabulary.app.data.repository

import com.vocabulary.app.data.model.ListOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One shared copy of the list settings, so Word List, Flashcards and Groupwise always use the
 * same filters. Kept in memory: the settings start fresh when the app is closed.
 */
@Singleton
class ListOptionsStore @Inject constructor() {

    private val _options = MutableStateFlow(ListOptions(shuffleSeed = System.nanoTime()))
    val options: StateFlow<ListOptions> = _options.asStateFlow()

    fun set(options: ListOptions) {
        _options.value = options
    }

    fun shuffleAgain() = _options.update { it.copy(shuffleSeed = System.nanoTime()) }

    fun reset() = _options.update { ListOptions(shuffleSeed = it.shuffleSeed, showStrip = it.showStrip, cardsStrip = it.cardsStrip, wordView = it.wordView) }
}
