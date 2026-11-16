package com.vocabulary.app.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.ListOptions
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.data.repository.ListOptionsStore
import com.vocabulary.app.data.repository.WordRepository
import com.vocabulary.app.domain.WordListFilter
import com.vocabulary.app.domain.initial
import com.vocabulary.app.tts.TtsManager
import com.vocabulary.app.tts.TtsStatus
import com.vocabulary.app.ui.list.WordFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read and favourite state at the moment the options last changed, so cards do not jump while you study. */
private data class CardsSnapshot(
    val options: ListOptions,
    val readIds: Set<Long>,
    val favouriteIds: Set<Long>
)

/**
 * The Flashcard page of the Words tab. It uses the same shared list settings as the Word List,
 * so a filter chosen on one page is still there on the other.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WordCardsViewModel @Inject constructor(
    private val repository: WordRepository,
    private val optionsStore: ListOptionsStore,
    private val tts: TtsManager
) : ViewModel() {

    /** Type and level live in the shared list settings, so every page keeps them. */
    val filters: StateFlow<WordFilters> = optionsStore.options
        .map { WordFilters(type = it.type, level = it.level) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            optionsStore.options.value.let { WordFilters(type = it.type, level = it.level) }
        )

    val options: StateFlow<ListOptions> = optionsStore.options

    val bookmarkedIds: StateFlow<Set<Long>> = repository.observeBookmarkedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val ttsStatus: StateFlow<TtsStatus> = tts.status

    private val allWords = repository.observeWords()

    private val snapshot = optionsStore.options.mapLatest { current ->
        CardsSnapshot(
            options = current,
            readIds = repository.observeReadIds().first().toSet(),
            favouriteIds = repository.observeBookmarkedIds().first().toSet()
        )
    }

    /** null means "still loading". */
    val words: StateFlow<List<WordEntity>?> = combine(allWords, snapshot) { list, state ->
        WordListFilter.apply(list, state.readIds, state.favouriteIds, state.options)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Letters that have a word under every other setting, for the sidebar's letter picker. */
    val availableLetters: StateFlow<Set<Char>> = combine(allWords, snapshot) { list, state ->
        WordListFilter.apply(
            list,
            state.readIds,
            state.favouriteIds,
            state.options.copy(letters = emptySet(), count = null)
        ).map { it.initial() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setOptions(options: ListOptions) = optionsStore.set(options)
    fun shuffleAgain() = optionsStore.shuffleAgain()
    fun resetOptions() = optionsStore.reset()

    fun toggleLetter(letter: Char) {
        val current = optionsStore.options.value
        val next = if (letter in current.letters) current.letters - letter else current.letters + letter
        optionsStore.set(current.copy(letters = next))
    }

    fun clearLetters() {
        optionsStore.set(optionsStore.options.value.copy(letters = emptySet()))
    }

    fun onTypeSelected(type: WordType?) = optionsStore.set(optionsStore.options.value.copy(type = type))
    fun onLevelSelected(level: Int?) = optionsStore.set(optionsStore.options.value.copy(level = level))

    fun toggleBookmark(wordId: Long) {
        viewModelScope.launch { repository.toggleBookmark(wordId) }
    }

    /** A card that is shown counts as read. */
    fun markRead(wordId: Long) {
        viewModelScope.launch { repository.markRead(wordId) }
    }

    fun speak(text: String) = tts.speak(text)

    override fun onCleared() {
        tts.stop()
    }
}
