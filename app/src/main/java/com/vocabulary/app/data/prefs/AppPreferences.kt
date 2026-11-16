package com.vocabulary.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vocabulary.app.data.model.QuizConfig
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.QuizSource
import com.vocabulary.app.data.model.QuizViewStyle
import com.vocabulary.app.data.model.ThemeMode
import com.vocabulary.app.domain.QuizLimits
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Daily word reminder settings. */
data class ReminderSettings(
    val enabled: Boolean = AppPreferences.DEFAULT_REMINDER_ENABLED,
    val hour: Int = AppPreferences.DEFAULT_REMINDER_HOUR,
    val minute: Int = AppPreferences.DEFAULT_REMINDER_MINUTE
)

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "vocabulary_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SEED_VERSION = intPreferencesKey("seed_version")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
        val GROUP_SHUFFLE_SEED = longPreferencesKey("group_shuffle_seed")
        val QUIZ_SOURCE = stringPreferencesKey("quiz_source")
        val QUIZ_MODES = stringPreferencesKey("quiz_modes")
        val QUIZ_COUNT = intPreferencesKey("quiz_count")
        val QUIZ_MINUTES = intPreferencesKey("quiz_minutes")
        val QUIZ_VIEW = stringPreferencesKey("quiz_view")
        val DAILY_WORD_DAY = longPreferencesKey("daily_word_day")
        val DAILY_WORD_ID = longPreferencesKey("daily_word_id")
    }

    /** Version of the bundled words.json that was last imported into the database. */
    val seedVersion: Flow<Int> = context.appDataStore.data.map { it[Keys.SEED_VERSION] ?: 0 }

    suspend fun setSeedVersion(version: Int) {
        context.appDataStore.edit { it[Keys.SEED_VERSION] = version }
    }

    /** Cards to answer per day to keep the streak. */
    val dailyGoal: Flow<Int> = context.appDataStore.data.map { it[Keys.DAILY_GOAL] ?: DEFAULT_DAILY_GOAL }

    suspend fun setDailyGoal(goal: Int) {
        context.appDataStore.edit { it[Keys.DAILY_GOAL] = goal.coerceAtLeast(1) }
    }

    val themeMode: Flow<ThemeMode> = context.appDataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    /** Daily word reminder on/off and time of day (device local time). */
    val reminder: Flow<ReminderSettings> = context.appDataStore.data.map {
        ReminderSettings(
            enabled = it[Keys.REMINDER_ENABLED] ?: DEFAULT_REMINDER_ENABLED,
            hour = it[Keys.REMINDER_HOUR] ?: DEFAULT_REMINDER_HOUR,
            minute = it[Keys.REMINDER_MINUTE] ?: DEFAULT_REMINDER_MINUTE
        )
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.appDataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.appDataStore.edit {
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    /** True once the notification permission dialog was shown, so it is never repeated. */
    val notificationPermissionAsked: Flow<Boolean> =
        context.appDataStore.data.map { it[Keys.NOTIFICATION_PERMISSION_ASKED] ?: false }

    suspend fun setNotificationPermissionAsked() {
        context.appDataStore.edit { it[Keys.NOTIFICATION_PERMISSION_ASKED] = true }
    }

    /** Seed of the shuffled word groups. Stays fixed until the user taps Reshuffle. */
    val groupShuffleSeed: Flow<Long> = context.appDataStore.data.map { it[Keys.GROUP_SHUFFLE_SEED] ?: 1L }

    suspend fun setGroupShuffleSeed(seed: Long) {
        context.appDataStore.edit { it[Keys.GROUP_SHUFFLE_SEED] = seed }
    }

    /** Last quiz setup choices, so the Quiz tab reopens as the user left it. */
    val quizConfig: Flow<QuizConfig> = context.appDataStore.data.map { prefs ->
        QuizConfig(
            source = prefs[Keys.QUIZ_SOURCE]
                ?.let { name -> QuizSource.entries.firstOrNull { it.name == name } }
                ?: QuizSource.ALL,
            modes = prefs[Keys.QUIZ_MODES]
                ?.split(",")
                ?.mapNotNull { name -> QuizMode.entries.firstOrNull { it.name == name } }
                ?.toSet()
                ?: setOf(QuizMode.MEANING),
            questionCount = QuizLimits.clampCount(prefs[Keys.QUIZ_COUNT] ?: QuizLimits.DEFAULT_COUNT),
            listTimeMinutes = QuizLimits.clampMinutes(prefs[Keys.QUIZ_MINUTES] ?: QuizLimits.DEFAULT_MINUTES),
            viewStyle = prefs[Keys.QUIZ_VIEW]
                ?.let { name -> QuizViewStyle.entries.firstOrNull { it.name == name } }
                ?: QuizViewStyle.CARD
        )
    }

    suspend fun setQuizConfig(config: QuizConfig) {
        context.appDataStore.edit {
            it[Keys.QUIZ_SOURCE] = config.source.name
            it[Keys.QUIZ_MODES] = config.modes.joinToString(",") { mode -> mode.name }
            it[Keys.QUIZ_COUNT] = QuizLimits.clampCount(config.questionCount)
            it[Keys.QUIZ_MINUTES] = QuizLimits.clampMinutes(config.listTimeMinutes)
            it[Keys.QUIZ_VIEW] = config.viewStyle.name
        }
    }

    /** Id of the word of the day chosen for [day] (epoch day), or null when none is pinned yet. */
    suspend fun pinnedDailyWord(day: Long): Long? {
        val prefs = context.appDataStore.data.first()
        return if (prefs[Keys.DAILY_WORD_DAY] == day) prefs[Keys.DAILY_WORD_ID] else null
    }

    /** Keeps today's word fixed, even when progress changes during the day. */
    suspend fun pinDailyWord(day: Long, wordId: Long) {
        context.appDataStore.edit {
            it[Keys.DAILY_WORD_DAY] = day
            it[Keys.DAILY_WORD_ID] = wordId
        }
    }

    companion object {
        const val DEFAULT_DAILY_GOAL = 10
        const val DEFAULT_REMINDER_ENABLED = true
        const val DEFAULT_REMINDER_HOUR = 8
        const val DEFAULT_REMINDER_MINUTE = 0
    }
}
