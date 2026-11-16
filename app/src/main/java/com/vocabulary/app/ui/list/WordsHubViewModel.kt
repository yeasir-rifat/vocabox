package com.vocabulary.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WordsHubViewModel @Inject constructor(
    repository: WordRepository
) : ViewModel() {

    val totalWords: StateFlow<Int> = repository.observeWordCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val readWords: StateFlow<Int> = repository.observeReadIds()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
