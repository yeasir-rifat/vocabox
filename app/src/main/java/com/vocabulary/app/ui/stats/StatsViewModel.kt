package com.vocabulary.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.local.entity.DailyActivityEntity
import com.vocabulary.app.data.model.WordStatus
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.data.repository.StatsRepository
import com.vocabulary.app.data.repository.WordRepository
import com.vocabulary.app.domain.DayUtils
import com.vocabulary.app.domain.StreakCalculator
import com.vocabulary.app.domain.StreakInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val activity: Map<Long, DailyActivityEntity> = emptyMap(),
    val today: Long = 0L,
    val streak: StreakInfo = StreakInfo(),
    val goal: Int = AppPreferences.DEFAULT_DAILY_GOAL,
    val totalWords: Int = 0,
    val masteredCount: Int = 0,
    val learningCount: Int = 0,
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0
) {
    val accuracyPercent: Int
        get() = if (totalAnswered == 0) 0 else totalCorrect * 100 / totalAnswered
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepository: StatsRepository,
    wordRepository: WordRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        statsRepository.observeActivity(),
        statsRepository.observeProgress(),
        wordRepository.observeWordCount(),
        statsRepository.dailyGoal
    ) { activity, progress, wordCount, goal ->
        val today = DayUtils.epochDay(System.currentTimeMillis())
        StatsUiState(
            activity = activity.associateBy { it.day },
            today = today,
            streak = StreakCalculator.calculate(activity, today),
            goal = goal,
            totalWords = wordCount,
            masteredCount = progress.count { it.status == WordStatus.MASTERED },
            learningCount = progress.count { it.status == WordStatus.LEARNING },
            totalAnswered = activity.sumOf { it.reviewed },
            totalCorrect = activity.sumOf { it.correct }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
