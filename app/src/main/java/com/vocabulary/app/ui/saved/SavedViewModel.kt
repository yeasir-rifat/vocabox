package com.vocabulary.app.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.local.entity.MistakeWithWord
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizSource
import com.vocabulary.app.data.repository.WordRepository
import com.vocabulary.app.domain.CustomDeck
import com.vocabulary.app.domain.QuizGroupRequest
import com.vocabulary.app.domain.QuizLauncher
import com.vocabulary.app.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: WordRepository,
    private val launcher: QuizLauncher,
    private val customDeck: CustomDeck,
    private val tts: TtsManager
) : ViewModel() {

    /** null means "still loading". */
    val bookmarks: StateFlow<List<WordEntity>?> = repository.observeBookmarkedWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val mistakes: StateFlow<List<MistakeWithWord>?> = repository.observeMistakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Star state for the word sheet, which also opens on the Mistakes list. */
    val bookmarkedIds: StateFlow<Set<Long>> = repository.observeBookmarkedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun removeBookmark(wordId: Long) {
        // Rows only exist for bookmarked words, so toggling removes the bookmark.
        viewModelScope.launch { repository.toggleBookmark(wordId) }
    }

    fun toggleBookmark(wordId: Long) {
        viewModelScope.launch { repository.toggleBookmark(wordId) }
    }

    fun restoreMistake(wordId: Long, count: Int) {
        viewModelScope.launch { repository.restoreMistake(wordId, count) }
    }

    fun removeMistake(wordId: Long) {
        viewModelScope.launch { repository.removeMistake(wordId) }
    }

    /** A word shown in the bottom sheet counts as read. */
    fun markRead(wordId: Long) {
        viewModelScope.launch { repository.markRead(wordId) }
    }

    fun speak(text: String) = tts.speak(text)

    /** Opens the Quiz tab on the Favorites source. The caller then switches to the tab. */
    fun quizFavorites() {
        launcher.request(QuizGroupRequest("Favorites", emptySet(), QuizSource.FAVORITES))
    }

    /** Opens the Quiz tab on the Mistakes source. The caller then switches to the tab. */
    fun quizMistakes() {
        launcher.request(QuizGroupRequest("Mistakes", emptySet(), QuizSource.MISTAKES))
    }

    /** Quiz on the words picked in the settings panel. */
    fun quizWords(label: String, ids: Set<Long>) {
        launcher.request(QuizGroupRequest(label, ids))
    }

    /** Leaves the picked words for the Flashcards screen, or clears them with null. */
    fun prepareFlashcards(ids: Set<Long>?) = customDeck.set(ids)

    override fun onCleared() {
        tts.stop()
    }
}
