package com.vocabulary.app.ui.list

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The older Type / Topic / Level filters of the Word List page. */
data class WordFilters(
    val type: WordType? = null,
    val topic: String? = null,
    val level: Int? = null
) {
    val isActive: Boolean get() = type != null || topic != null || level != null
}

/**
 * Read and favourite state at the moment the options last changed. Words are filtered against
 * this copy, so a word you open from the Unread list stays where it is instead of vanishing
 * (and pulling the rows below it up) the moment it becomes read. Changing any option takes a
 * fresh copy.
 */
private data class OptionsSnapshot(
    val options: ListOptions,
    val readIds: Set<Long>,
    val favouriteIds: Set<Long>
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class WordsViewModel @Inject constructor(
    private val repository: WordRepository,
    private val optionsStore: ListOptionsStore,
    private val tts: TtsManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Type and level now live in the shared list settings, so every page keeps them. */
    val filters: StateFlow<WordFilters> = optionsStore.options
        .map { WordFilters(type = it.type, level = it.level) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            optionsStore.options.value.let { WordFilters(type = it.type, level = it.level) }
        )

    /** Shared list settings: read state, favourites, order and how many words to show. */
    val options: StateFlow<ListOptions> = optionsStore.options

    val topics: StateFlow<List<String>> = repository.observeTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarkedIds: StateFlow<Set<Long>> = repository.observeBookmarkedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val searchResults = _query
        .debounce { if (it.isEmpty()) 0L else 200L }
        .flatMapLatest { query -> repository.observeWords(query) }

    private val snapshot = optionsStore.options.mapLatest { current ->
        OptionsSnapshot(
            options = current,
            readIds = repository.observeReadIds().first().toSet(),
            favouriteIds = repository.observeBookmarkedIds().first().toSet()
        )
    }

    /** null means "still loading". */
    val words: StateFlow<List<WordEntity>?> = combine(searchResults, snapshot) { list, state ->
        WordListFilter.apply(list, state.readIds, state.favouriteIds, state.options)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Letters that have at least one word under every other setting. The letter filter itself and
     * the word count are left out, so the strip can still show letters you could add.
     */
    val availableLetters: StateFlow<Set<Char>> = combine(searchResults, snapshot) { list, state ->
        WordListFilter.apply(
            list,
            state.readIds,
            state.favouriteIds,
            state.options.copy(letters = emptySet(), count = null)
        ).map { it.initial() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleLetter(letter: Char) {
        val current = optionsStore.options.value
        val next = if (letter in current.letters) current.letters - letter else current.letters + letter
        optionsStore.set(current.copy(letters = next))
    }

    fun clearLetters() {
        optionsStore.set(optionsStore.options.value.copy(letters = emptySet()))
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onTypeSelected(type: WordType?) = optionsStore.set(optionsStore.options.value.copy(type = type))
    fun onLevelSelected(level: Int?) = optionsStore.set(optionsStore.options.value.copy(level = level))

    fun setOptions(options: ListOptions) = optionsStore.set(options)
    fun shuffleAgain() = optionsStore.shuffleAgain()
    fun resetOptions() = optionsStore.reset()

    fun toggleBookmark(wordId: Long) {
        viewModelScope.launch { repository.toggleBookmark(wordId) }
    }

    /** A word shown in the bottom sheet counts as read. */
    fun markRead(wordId: Long) {
        viewModelScope.launch { repository.markRead(wordId) }
    }

    fun speak(text: String) = tts.speak(text)

    override fun onCleared() {
        tts.stop()
    }
}
