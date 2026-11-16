package com.vocabulary.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.WordStatus
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.data.repository.StatsRepository
import com.vocabulary.app.data.repository.StudyRepository
import com.vocabulary.app.data.repository.WordRepository
import com.vocabulary.app.domain.DailyWordPicker
import com.vocabulary.app.domain.DayTicker
import com.vocabulary.app.domain.DayUtils
import com.vocabulary.app.domain.StreakCalculator
import com.vocabulary.app.domain.StreakInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val dailyWord: WordEntity? = null,
    val streak: StreakInfo = StreakInfo(),
    val todayCount: Int = 0,
    val goal: Int = AppPreferences.DEFAULT_DAILY_GOAL,
    val dueCount: Int = 0,
    val mistakeCount: Int = 0,
    val masteredCount: Int = 0,
    val totalWords: Int = 0,
    val activity: Map<Long, Int> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    wordRepository: WordRepository,
    statsRepository: StatsRepository,
    studyRepository: StudyRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    // Restarts at every midnight, so due counts, today's progress and the word of the day never go stale.
    val uiState: StateFlow<HomeUiState> = DayTicker.dayStarts().flatMapLatest { dayStart ->
        combine(
            wordRepository.observeWords(),
            statsRepository.observeProgress(),
            statsRepository.observeActivity(),
            statsRepository.dailyGoal,
            combine(
                studyRepository.observeDueCount(dayStart, null, null, null),
                studyRepository.observeMistakeCount()
            ) { due, mistakes -> due to mistakes }
        ) { words, progress, activity, goal, counts ->
            val today = DayUtils.epochDay(dayStart)
            val pinnedId = preferences.pinnedDailyWord(today)
            val dailyWord = words.firstOrNull { it.id == pinnedId }
                ?: DailyWordPicker.pick(words, progress.associateBy { it.wordId }, today)
                    ?.also { preferences.pinDailyWord(today, it.id) }
            HomeUiState(
                dailyWord = dailyWord,
                streak = StreakCalculator.calculate(activity, today),
                todayCount = activity.firstOrNull { it.day == today }?.reviewed ?: 0,
                goal = goal,
                dueCount = counts.first,
                mistakeCount = counts.second,
                masteredCount = progress.count { it.status == WordStatus.MASTERED },
                totalWords = words.size,
                activity = activity.associate { it.day to it.reviewed }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
