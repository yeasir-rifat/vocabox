package com.vocabulary.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.repository.WordRepository
import com.vocabulary.app.tts.TtsManager
import com.vocabulary.app.tts.TtsStatus
import com.vocabulary.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WordDetailUiState {
    data object Loading : WordDetailUiState
    data object NotFound : WordDetailUiState
    data class Found(val word: WordEntity) : WordDetailUiState
}

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WordRepository,
    private val tts: TtsManager
) : ViewModel() {

    private val wordId: Long = checkNotNull(savedStateHandle[Screen.WordDetail.ARG_WORD_ID])

    val uiState: StateFlow<WordDetailUiState> = repository.observeWord(wordId)
        .map { word ->
            if (word == null) WordDetailUiState.NotFound else WordDetailUiState.Found(word)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordDetailUiState.Loading)

    val isBookmarked: StateFlow<Boolean> = repository.observeIsBookmarked(wordId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val ttsStatus: StateFlow<TtsStatus> = tts.status

    init {
        // Opening the detail page counts as reading the word.
        viewModelScope.launch { repository.markRead(wordId) }
    }

    fun toggleBookmark() {
        viewModelScope.launch { repository.toggleBookmark(wordId) }
    }

    fun speak(text: String) = tts.speak(text)

    override fun onCleared() {
        tts.stop()
    }
}
