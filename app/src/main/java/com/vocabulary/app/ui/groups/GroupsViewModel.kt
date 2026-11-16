package com.vocabulary.app.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.ListOptions
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.data.repository.GroupsState
import com.vocabulary.app.data.repository.ListOptionsStore
import com.vocabulary.app.data.repository.WordGroupsSource
import com.vocabulary.app.data.repository.WordRepository
import com.vocabulary.app.domain.QuizGroupRequest
import com.vocabulary.app.domain.QuizLauncher
import com.vocabulary.app.domain.WordGroup
import com.vocabulary.app.tts.TtsManager
import com.vocabulary.app.ui.list.WordFilters
import com.vocabulary.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val repository: WordRepository,
    private val optionsStore: ListOptionsStore,
    source: WordGroupsSource,
    private val launcher: QuizLauncher
) : ViewModel() {

    /** The same list settings as the Word List and the Flashcard page. */
    val options: StateFlow<ListOptions> = optionsStore.options

    /** Type and level live in the shared list settings. */
    val filters: StateFlow<WordFilters> = optionsStore.options
        .map { WordFilters(type = it.type, level = it.level) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            optionsStore.options.value.let { WordFilters(type = it.type, level = it.level) }
        )

    /** null while loading. */
    val state: StateFlow<GroupsState?> = source.state

    val readIds: StateFlow<Set<Long>> = repository.observeReadIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

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

    fun quiz(group: WordGroup) {
        launcher.request(QuizGroupRequest(group.title, group.words.mapTo(HashSet()) { it.id }))
    }
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WordRepository,
    source: WordGroupsSource,
    private val launcher: QuizLauncher,
    private val tts: TtsManager
) : ViewModel() {

    private val index: Int = savedStateHandle.get<Int>(Screen.GroupDetail.ARG_INDEX) ?: 0

    /** null while loading, or when the group no longer exists (for example after the settings changed). */
    val group: StateFlow<WordGroup?> = source.state
        .map { it?.groups?.getOrNull(index) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val readIds: StateFlow<Set<Long>> = repository.observeReadIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun quiz(group: WordGroup) {
        launcher.request(QuizGroupRequest(group.title, group.words.mapTo(HashSet()) { it.id }))
    }

    /** Every word, used to find related entries that sit in other groups. */
    val categoryWords: StateFlow<List<WordEntity>> = repository.observeWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarkedIds: StateFlow<Set<Long>> = repository.observeBookmarkedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleBookmark(wordId: Long) {
        viewModelScope.launch { repository.toggleBookmark(wordId) }
    }

    /** Opening a word card counts as reading the word. */
    fun markRead(wordId: Long) {
        viewModelScope.launch { repository.markRead(wordId) }
    }

    fun speak(text: String) = tts.speak(text)

    fun stopSpeaking() = tts.stop()

    override fun onCleared() {
        tts.stop()
    }
}
