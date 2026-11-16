package com.vocabulary.app.ui.flashcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.DeckSource
import com.vocabulary.app.data.repository.DeckSpec
import com.vocabulary.app.data.repository.StudyRepository
import com.vocabulary.app.data.repository.WordRepository
import com.vocabulary.app.domain.CustomDeck
import com.vocabulary.app.domain.DayTicker
import com.vocabulary.app.tts.TtsManager
import com.vocabulary.app.tts.TtsStatus
import com.vocabulary.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Choices made on the setup screen. */
data class DeckConfig(
    val source: DeckSource = DeckSource.SMART,
    val size: Int = DEFAULT_SIZE,
    val level: Int? = null
) {
    companion object {
        const val DEFAULT_SIZE = 20
        val SIZES = listOf(10, 20, 30)
    }
}

/** How many cards each source could provide right now. */
data class DeckCounts(
    val due: Int = 0,
    val fresh: Int = 0,
    val bookmarked: Int = 0,
    val mistakes: Int = 0
) {
    fun available(source: DeckSource): Int = when (source) {
        DeckSource.SMART -> due + fresh
        DeckSource.BOOKMARKED -> bookmarked
        DeckSource.MISTAKES -> mistakes
    }
}

sealed interface SessionState {
    data object Setup : SessionState
    data object Loading : SessionState
    data object Empty : SessionState

    /**
     * [resolved] counts cards that have left the queue. [serial] changes after every answer,
     * so the UI can reset card state even when the same word comes back immediately.
     */
    data class Active(
        val current: WordEntity,
        val hasNext: Boolean,
        val total: Int,
        val resolved: Int,
        val serial: Int
    ) : SessionState

    data class Finished(
        val total: Int,
        val knownFirstTry: Int,
        val missed: List<WordEntity>
    ) : SessionState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val studyRepository: StudyRepository,
    private val wordRepository: WordRepository,
    private val customDeck: CustomDeck,
    private val tts: TtsManager
) : ViewModel() {

    /** Words chosen in the Saved settings panel, or null for a normal deck. */
    private val customIds: Set<Long>? = customDeck.take()

    private val _config = MutableStateFlow(
        DeckConfig(
            source = savedStateHandle.get<String>(Screen.Flashcards.ARG_SOURCE)
                ?.let { name -> DeckSource.entries.firstOrNull { it.name == name } }
                ?: DeckSource.SMART
        )
    )
    val config: StateFlow<DeckConfig> = _config.asStateFlow()

    private val _session = MutableStateFlow<SessionState>(SessionState.Setup)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    val bookmarkedIds: StateFlow<Set<Long>> = wordRepository.observeBookmarkedIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val counts: StateFlow<DeckCounts> = combine(
        _config.map { it.level }.distinctUntilChanged(),
        DayTicker.dayStarts()
    ) { level, dayStart -> level to dayStart }
        .flatMapLatest { (level, dayStart) ->
            combine(
                studyRepository.observeDueCount(dayStart, null, level, null),
                studyRepository.observeNewCount(null, level, null),
                studyRepository.observeBookmarkCount(),
                studyRepository.observeMistakeCount()
            ) { due, fresh, bookmarked, mistakes ->
                val custom = customIds?.size
                if (custom != null) DeckCounts(due, fresh, custom, custom)
                else DeckCounts(due, fresh, bookmarked, mistakes)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeckCounts())

    val ttsStatus: StateFlow<TtsStatus> = tts.status

    // Session bookkeeping. A card answered "unknown" goes back into the queue, but only the
    // first answer for a word changes its spaced-repetition schedule.
    private val queue = ArrayDeque<WordEntity>()
    private val graded = mutableSetOf<Long>()
    private val missed = LinkedHashMap<Long, WordEntity>()
    private var total = 0
    private var resolved = 0
    private var knownFirstTry = 0
    private var serial = 0

    fun onSourceSelected(source: DeckSource) = _config.update { it.copy(source = source) }
    fun onSizeSelected(size: Int) = _config.update { it.copy(size = size) }
    fun onLevelSelected(level: Int?) = _config.update { it.copy(level = level) }

    fun start() {
        if (_session.value == SessionState.Loading) return
        _session.value = SessionState.Loading
        viewModelScope.launch {
            val cfg = _config.value
            val deck = studyRepository.buildDeck(
                DeckSpec(cfg.source, cfg.size, null, null, cfg.level, customIds)
            )
            if (deck.isEmpty()) {
                _session.value = SessionState.Empty
                return@launch
            }
            queue.clear()
            queue.addAll(deck)
            graded.clear()
            missed.clear()
            total = deck.size
            resolved = 0
            knownFirstTry = 0
            serial = 0
            publish()
        }
    }

    fun onAnswer(known: Boolean) {
        if (_session.value !is SessionState.Active) return
        val card = queue.removeFirstOrNull() ?: return

        if (graded.add(card.id)) {
            if (known) knownFirstTry++ else missed[card.id] = card
            viewModelScope.launch {
                // Make sure the last answer is saved even if the screen is closed right away.
                withContext(NonCancellable) { studyRepository.grade(card.id, known) }
            }
        }

        if (known) {
            resolved++
        } else {
            queue.add(minOf(RETRY_GAP, queue.size), card)
        }
        serial++
        publish()
    }

    fun backToSetup() {
        _session.value = SessionState.Setup
    }

    fun toggleBookmark(wordId: Long) {
        viewModelScope.launch { wordRepository.toggleBookmark(wordId) }
    }

    fun speak(text: String) = tts.speak(text)

    private fun publish() {
        val current = queue.firstOrNull()
        // A card that is shown counts as read.
        current?.let { card -> viewModelScope.launch { wordRepository.markRead(card.id) } }
        _session.value = if (current == null) {
            SessionState.Finished(total, knownFirstTry, missed.values.toList())
        } else {
            SessionState.Active(
                current = current,
                hasNext = queue.size > 1,
                total = total,
                resolved = resolved,
                serial = serial
            )
        }
    }

    override fun onCleared() {
        tts.stop()
    }

    private companion object {
        /** A missed card comes back after this many other cards. */
        const val RETRY_GAP = 3
    }
}
