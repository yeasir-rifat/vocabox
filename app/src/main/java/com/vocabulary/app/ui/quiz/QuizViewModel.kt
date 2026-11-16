package com.vocabulary.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizConfig
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.QuizSource
import com.vocabulary.app.data.model.QuizViewStyle
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.data.repository.QuizRepository
import com.vocabulary.app.data.repository.WordRepository
import com.vocabulary.app.domain.QuizAvailability
import com.vocabulary.app.domain.QuizGenerator
import com.vocabulary.app.domain.QuizGroupRequest
import com.vocabulary.app.domain.QuizLauncher
import com.vocabulary.app.domain.QuizLimits
import com.vocabulary.app.domain.QuizQuestion
import com.vocabulary.app.domain.QuizScoring
import com.vocabulary.app.ui.quiz.report.ReportOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** [selected] is [TIMED_OUT] / [UNANSWERED] when the user gave no answer. */
data class AnsweredQuestion(val question: QuizQuestion, val selected: Int) {
    val selectedText: String? get() = question.options.getOrNull(selected)
    val isCorrect: Boolean get() = selected == question.correctIndex
}

data class QuizSummary(
    val modes: List<QuizMode>,
    val total: Int,
    val correct: Int,
    val durationSec: Int,
    /** Every question in quiz order, with the user's answer. */
    val items: List<AnsweredQuestion>,
    /** Name of the word group when the quiz came from one, else null. Used in the PDF heading. */
    val groupLabel: String? = null
) {
    val wrong: List<AnsweredQuestion> get() = items.filter { !it.isCorrect }
}

const val TIMED_OUT = -1
const val UNANSWERED = TIMED_OUT

enum class QuizKind { STEP, LIST }

/** Everything the setup screen needs, already combined. */
data class QuizSetupUi(
    val config: QuizConfig,
    val perMode: Map<QuizMode, Int>,
    /** Selected modes that have at least one question, in QuizMode order. */
    val effectiveModes: List<QuizMode>,
    /** Questions a quiz would have: min(requested, distinct words available). */
    val questionCount: Int,
    val sourceCounts: Map<QuizSource, Int>,
    /** Name of the word group sent here from the Groups screen, or null. */
    val groupLabel: String? = null
) {
    val enabledModes: List<QuizMode> get() = QuizMode.entries.filter { (perMode[it] ?: 0) > 0 }
    val canStart: Boolean get() = effectiveModes.isNotEmpty() && questionCount > 0
}

sealed interface QuizState {
    data object Setup : QuizState
    data object Loading : QuizState
    data object Empty : QuizState

    /** Step Quiz: one question at a time. */
    data class Active(
        val question: QuizQuestion,
        val index: Int,
        val total: Int,
        val selected: Int?,
        val timeLeft: Int
    ) : QuizState {
        val isLast: Boolean get() = index + 1 >= total
    }

    /** List Quiz: all questions on one page. [timeLeftSec] is null when there is no limit. */
    data class ListActive(
        val questions: List<QuizQuestion>,
        val answers: Map<Int, Int>,
        val timeLeftSec: Int?
    ) : QuizState {
        val answered: Int get() = answers.size
        val unanswered: Int get() = questions.size - answers.size
    }

    data class Finished(val summary: QuizSummary) : QuizState

    /** PDF preview / customize screen opened from the result. */
    data class Report(val summary: QuizSummary) : QuizState
}

private class Sources(
    val all: List<WordEntity>,
    val favorites: List<WordEntity>,
    val mistakes: List<WordEntity>
) {
    fun targets(source: QuizSource, groupIds: Set<Long>): List<WordEntity> = when (source) {
        QuizSource.ALL -> all
        QuizSource.FAVORITES -> favorites
        QuizSource.MISTAKES -> mistakes
        QuizSource.GROUP -> all.filter { it.id in groupIds }
    }
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    private val preferences: AppPreferences,
    private val launcher: QuizLauncher
) : ViewModel() {

    private val _config = MutableStateFlow(QuizConfig())
    val config: StateFlow<QuizConfig> = _config.asStateFlow()

    private val _reportOptions = MutableStateFlow(ReportOptions())
    val reportOptions: StateFlow<ReportOptions> = _reportOptions.asStateFlow()

    private val _state = MutableStateFlow<QuizState>(QuizState.Loading)
    val state: StateFlow<QuizState> = _state.asStateFlow()

    private val sourcesFlow = combine(
        wordRepository.observeWords(),
        wordRepository.observeBookmarkedWords(),
        wordRepository.observeMistakeWords()
    ) { all, favorites, mistakes -> Sources(all, favorites, mistakes) }

    private val groupTarget = MutableStateFlow<QuizGroupRequest?>(null)

    /** Per mode availability for the chosen source. Updates instantly when the source changes. */
    val availability: StateFlow<QuizAvailability> = combine(
        sourcesFlow,
        _config.map { it.source }.distinctUntilChanged(),
        groupTarget
    ) { sources, source, group ->
        val targets = sources.targets(source, group?.wordIds.orEmpty())
        QuizAvailability(QuizGenerator.buildableIds(targets, sources.all), true)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuizAvailability())

    /** Null until the saved config and the counts are loaded. */
    val setup: StateFlow<QuizSetupUi?> = combine(_config, availability, sourcesFlow, groupTarget) { cfg, avail, sources, group ->
        if (!avail.loaded) return@combine null
        val effective = QuizMode.entries.filter { it in cfg.modes && avail.count(it) > 0 }
        QuizSetupUi(
            config = cfg,
            perMode = QuizMode.entries.associateWith { avail.count(it) },
            effectiveModes = effective,
            questionCount = QuizLimits.effectiveCount(cfg.questionCount, avail.totalFor(effective)),
            sourceCounts = QuizSource.entries.associateWith {
                sources.targets(it, group?.wordIds.orEmpty()).size
            },
            groupLabel = group?.label
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var questions: List<QuizQuestion> = emptyList()
    private var activeModes: List<QuizMode> = emptyList()
    private var lastKind = QuizKind.STEP
    private var index = 0
    private val answered = mutableListOf<AnsweredQuestion>()
    private var startedAt = 0L
    private var timerJob: Job? = null

    /** Wall clock deadlines, so the countdown stays right after rotation or a stalled frame. */
    private var questionDeadline = 0L
    private var listAnswers: Map<Int, Int> = emptyMap()
    private var listDeadline: Long? = null

    init {
        viewModelScope.launch {
            val saved = preferences.quizConfig.first()
            // A group only lives until the app closes, so a saved GROUP source falls back to All.
            _config.value = if (saved.source == QuizSource.GROUP) saved.copy(source = QuizSource.ALL) else saved
            if (_state.value == QuizState.Loading) _state.value = QuizState.Setup
            // "Quiz this group" from the Groups screens.
            launcher.pending.filterNotNull().collect { request ->
                // Only a group brings its own words. Favorites and Mistakes read the live lists.
                groupTarget.value = if (request.source == QuizSource.GROUP) request else null
                timerJob?.cancel()
                _state.value = QuizState.Setup
                updateConfig { it.copy(source = request.source) }
                launcher.consume()
            }
        }
    }

    // ---- Setup -------------------------------------------------------------------------

    private fun updateConfig(block: (QuizConfig) -> QuizConfig) {
        val updated = block(_config.value)
        _config.value = updated
        viewModelScope.launch { preferences.setQuizConfig(updated) }
    }

    fun onViewStyle(style: QuizViewStyle) = updateConfig { it.copy(viewStyle = style) }
    fun onSource(source: QuizSource) = updateConfig { it.copy(source = source) }
    fun onCount(count: Int) = updateConfig { it.copy(questionCount = QuizLimits.clampCount(count)) }
    fun onListMinutes(minutes: Int) = updateConfig { it.copy(listTimeMinutes = QuizLimits.clampMinutes(minutes)) }

    fun onToggleMode(mode: QuizMode) = updateConfig {
        it.copy(modes = if (mode in it.modes) it.modes - mode else it.modes + mode)
    }

    /** Selects every enabled mode, or clears the selection when they are all selected already. */
    fun onToggleAllModes() {
        val ui = setup.value ?: return
        val allSelected = ui.enabledModes.isNotEmpty() && ui.effectiveModes.size == ui.enabledModes.size
        updateConfig { it.copy(modes = if (allSelected) emptySet() else ui.enabledModes.toSet()) }
    }

    // ---- Starting ----------------------------------------------------------------------

    fun startStep() = start(QuizKind.STEP)
    fun startList() = start(QuizKind.LIST)
    fun retry() = start(lastKind)

    private fun start(kind: QuizKind) {
        val ui = setup.value ?: return
        if (_state.value == QuizState.Loading || ui.effectiveModes.isEmpty()) return
        timerJob?.cancel()
        pausedRemainingMs = null
        lastKind = kind
        _reportOptions.update { it.copy(title = "", groupText = "") }
        _state.value = QuizState.Loading
        viewModelScope.launch {
            val cfg = _config.value
            val sources = sourcesFlow.first()
            val modes = ui.effectiveModes
            val generated = withContext(Dispatchers.Default) {
                QuizGenerator.generateMixed(
                    modes = modes,
                    targets = sources.targets(cfg.source, groupTarget.value?.wordIds.orEmpty()),
                    total = cfg.questionCount,
                    pool = sources.all
                )
            }
            if (generated.isEmpty()) {
                _state.value = QuizState.Empty
                return@launch
            }
            questions = generated
            activeModes = modes
            index = 0
            pausedStepMs = null
            answered.clear()
            startedAt = System.currentTimeMillis()
            when (kind) {
                QuizKind.STEP -> showQuestion()
                QuizKind.LIST -> beginList(cfg.listTimeMinutes)
            }
        }
    }

    // ---- Step Quiz ---------------------------------------------------------------------

    fun onAnswer(selected: Int) {
        val current = _state.value as? QuizState.Active ?: return
        if (current.selected != null) return
        timerJob?.cancel()

        val question = current.question
        val isCorrect = selected == question.correctIndex
        answered += AnsweredQuestion(question, selected)
        _state.value = current.copy(selected = selected)

        viewModelScope.launch {
            withContext(NonCancellable) { quizRepository.recordAnswer(question.wordId, isCorrect) }
        }
    }

    fun onNext() {
        val current = _state.value as? QuizState.Active ?: return
        if (current.selected == null) return
        if (current.index < index) {
            // Looking at an earlier question: go one forward.
            showAt(current.index + 1)
            return
        }
        if (current.isLast) {
            finish()
        } else {
            index++
            showQuestion()
        }
    }

    /** Time left of the question in play while an earlier question is on screen. */
    private var pausedStepMs: Long? = null

    /** Goes back one question to look at it again. The clock of an unanswered question waits. */
    fun onPrevious() {
        val current = _state.value as? QuizState.Active ?: return
        if (current.index == 0) return
        if (current.index == index && current.selected == null) {
            pausedStepMs = (questionDeadline - System.currentTimeMillis()).coerceAtLeast(1_000L)
            timerJob?.cancel()
        }
        showAt(current.index - 1)
    }

    /** Shows question [i] (at most the one in play): answered ones read-only, the open one with its clock. */
    private fun showAt(i: Int) {
        val answeredItem = answered.getOrNull(i)
        if (answeredItem != null) {
            _state.value = QuizState.Active(
                question = questions[i],
                index = i,
                total = questions.size,
                selected = answeredItem.selected,
                timeLeft = 0
            )
        } else {
            val ms = pausedStepMs ?: (QuizLimits.STEP_SECONDS_PER_QUESTION * 1_000L)
            pausedStepMs = null
            questionDeadline = System.currentTimeMillis() + ms
            _state.value = QuizState.Active(
                question = questions[i],
                index = i,
                total = questions.size,
                selected = null,
                timeLeft = secondsLeft(questionDeadline)
            )
            runStepTimer(i)
        }
    }

    private fun showQuestion() {
        val seconds = QuizLimits.STEP_SECONDS_PER_QUESTION
        questionDeadline = System.currentTimeMillis() + seconds * 1_000L
        _state.value = QuizState.Active(
            question = questions[index],
            index = index,
            total = questions.size,
            selected = null,
            timeLeft = seconds
        )
        runStepTimer(index)
    }

    private fun runStepTimer(shownIndex: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val left = secondsLeft(questionDeadline)
                _state.update { s ->
                    if (s is QuizState.Active && s.index == shownIndex && s.selected == null && s.timeLeft != left) {
                        s.copy(timeLeft = left)
                    } else {
                        s
                    }
                }
                if (left <= 0) break
                delay(TICK_MS)
            }
            onAnswer(TIMED_OUT)
        }
    }

    // ---- List Quiz ---------------------------------------------------------------------

    private fun beginList(minutes: Int) {
        listAnswers = emptyMap()
        listDeadline = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else null
        _state.value = QuizState.ListActive(
            questions = questions,
            answers = emptyMap(),
            timeLeftSec = listDeadline?.let { secondsLeft(it) }
        )
        listDeadline?.let { runListTimer(it) }
    }

    private fun runListTimer(deadline: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val left = secondsLeft(deadline)
                _state.update { s ->
                    if (s is QuizState.ListActive && s.timeLeftSec != left) s.copy(timeLeftSec = left) else s
                }
                if (left <= 0) break
                delay(TICK_MS)
            }
            submitList()
        }
    }

    // ---- App in the background -----------------------------------------------------------

    private var pausedRemainingMs: Long? = null

    /** The app left the screen (home button, call). Stops the clock so no question times out unseen. */
    fun onAppPaused() {
        val running = timerJob?.isActive == true
        if (!running) return
        when (val s = _state.value) {
            is QuizState.Active -> if (s.selected == null) {
                pausedRemainingMs = (questionDeadline - System.currentTimeMillis()).coerceAtLeast(1_000L)
                timerJob?.cancel()
            }
            is QuizState.ListActive -> listDeadline?.let { deadline ->
                pausedRemainingMs = (deadline - System.currentTimeMillis()).coerceAtLeast(1_000L)
                timerJob?.cancel()
            }
            else -> Unit
        }
    }

    /** The app is back: the clock continues with the time that was left. */
    fun onAppResumed() {
        val remaining = pausedRemainingMs ?: return
        pausedRemainingMs = null
        when (val s = _state.value) {
            is QuizState.Active -> if (s.selected == null) {
                questionDeadline = System.currentTimeMillis() + remaining
                runStepTimer(s.index)
            }
            is QuizState.ListActive -> if (listDeadline != null) {
                val deadline = System.currentTimeMillis() + remaining
                listDeadline = deadline
                runListTimer(deadline)
            }
            else -> Unit
        }
    }

    /** The user can change an answer until the quiz is submitted. */
    fun onSelectListAnswer(questionIndex: Int, option: Int) {
        _state.update { s ->
            if (s is QuizState.ListActive && questionIndex in s.questions.indices) {
                s.copy(answers = s.answers + (questionIndex to option))
            } else {
                s
            }
        }
    }

    fun submitList() {
        val current = _state.value as? QuizState.ListActive ?: return
        timerJob?.cancel()
        val items = current.questions.mapIndexed { i, q -> AnsweredQuestion(q, current.answers[i] ?: UNANSWERED) }
        val correct = QuizScoring.correctCount(current.questions, current.answers)
        val now = System.currentTimeMillis()
        val summary = QuizSummary(
            modes = activeModes,
            total = items.size,
            correct = correct,
            durationSec = ((now - startedAt) / 1_000).toInt(),
            items = items,
            groupLabel = summaryGroupLabel()
        )
        _state.value = QuizState.Finished(summary)
        viewModelScope.launch {
            withContext(NonCancellable) {
                // Unanswered questions count as wrong, so they also go to the Mistakes list.
                items.forEach { quizRepository.recordAnswer(it.question.wordId, it.isCorrect, now) }
                saveSummary(summary, now)
            }
        }
    }

    // ---- Common ------------------------------------------------------------------------

    private fun summaryGroupLabel(): String? =
        if (_config.value.source == QuizSource.GROUP) groupTarget.value?.label else null

    // ---- PDF report --------------------------------------------------------------------

    fun openReport() {
        val finished = _state.value as? QuizState.Finished ?: return
        _state.value = QuizState.Report(finished.summary)
    }

    fun closeReport() {
        val report = _state.value as? QuizState.Report ?: return
        _state.value = QuizState.Finished(report.summary)
    }

    fun onReportOptions(options: ReportOptions) {
        _reportOptions.value = options
    }

    fun backToSetup() {
        timerJob?.cancel()
        pausedRemainingMs = null
        _state.value = QuizState.Setup
    }

    private fun finish() {
        val now = System.currentTimeMillis()
        val summary = QuizSummary(
            modes = activeModes,
            total = questions.size,
            correct = answered.count { it.isCorrect },
            durationSec = ((now - startedAt) / 1_000).toInt(),
            items = answered.toList(),
            groupLabel = summaryGroupLabel()
        )
        _state.value = QuizState.Finished(summary)
        viewModelScope.launch { withContext(NonCancellable) { saveSummary(summary, now) } }
    }

    /** One quiz_results row per mode, so a mixed quiz still counts toward each mode it contained. */
    private suspend fun saveSummary(summary: QuizSummary, now: Long) {
        val byMode = summary.items.groupBy { it.question.mode }
        if (byMode.isEmpty()) {
            quizRepository.saveResult(
                mode = summary.modes.firstOrNull() ?: QuizMode.MEANING,
                total = summary.total,
                correct = summary.correct,
                durationSec = summary.durationSec,
                takenAt = now
            )
            return
        }
        byMode.forEach { (mode, items) ->
            quizRepository.saveResult(
                mode = mode,
                total = items.size,
                correct = items.count { it.isCorrect },
                durationSec = summary.durationSec,
                takenAt = now
            )
        }
    }

    private fun secondsLeft(deadline: Long): Int {
        val ms = deadline - System.currentTimeMillis()
        return if (ms <= 0) 0 else ((ms + 999) / 1_000).toInt()
    }

    private companion object {
        const val TICK_MS = 200L
    }
}
