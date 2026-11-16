package com.vocabulary.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.model.ThemeMode
import com.vocabulary.app.data.auth.AuthRepository
import com.vocabulary.app.data.auth.AuthState
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.data.remote.RemoteWordSync
import com.vocabulary.app.data.remote.SyncState
import com.vocabulary.app.data.prefs.ReminderSettings
import com.vocabulary.app.data.repository.SettingsRepository
import com.vocabulary.app.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reminder: ReminderSettings = ReminderSettings(),
    val dailyGoal: Int = AppPreferences.DEFAULT_DAILY_GOAL,
    val userName: String = "",
    val userEmail: String = "",
    val sync: SyncState = SyncState.Idle,
    val lastSyncMillis: Long = 0L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val scheduler: ReminderScheduler,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val wordSync: RemoteWordSync
) : ViewModel() {

    private val base = combine(
        preferences.themeMode,
        preferences.reminder,
        preferences.dailyGoal
    ) { theme, reminder, goal ->
        SettingsUiState(themeMode = theme, reminder = reminder, dailyGoal = goal)
    }

    private val account = combine(
        authRepository.state,
        wordSync.state,
        preferences.lastSyncMillis
    ) { auth, sync, last -> Triple(auth, sync, last) }

    val uiState: StateFlow<SettingsUiState> = combine(base, account) { state, (auth, sync, last) ->
        val user = auth as? AuthState.SignedIn
        state.copy(
            userName = user?.name.orEmpty(),
            userEmail = user?.email.orEmpty(),
            sync = sync,
            lastSyncMillis = last
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun syncNow() {
        viewModelScope.launch { wordSync.sync(force = true) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setReminderEnabled(enabled)
            scheduler.reschedule()
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferences.setReminderTime(hour, minute)
            scheduler.reschedule()
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { preferences.setDailyGoal(goal) }
    }

    fun resetProgress(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.resetProgress()
            onDone()
        }
    }
}
