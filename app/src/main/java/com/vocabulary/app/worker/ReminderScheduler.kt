package com.vocabulary.app.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.domain.ReminderTime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the daily word work in sync with the reminder settings.
 *
 * Each run plans the next one for the next local HH:mm ([scheduleNext]). That keeps the time right
 * across Doze, daylight saving and time zone changes, which a fixed 24 hour period would not.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences
) {

    /** Call on app start. Keeps an existing plan untouched. */
    suspend fun ensureScheduled() = apply(ExistingWorkPolicy.KEEP)

    /** Call after the reminder switch, the time, the time zone or the clock changes. */
    suspend fun reschedule() = apply(ExistingWorkPolicy.REPLACE)

    /** Called by the worker itself, so it must not cancel the run that is calling it. */
    suspend fun scheduleNext() = apply(ExistingWorkPolicy.APPEND_OR_REPLACE)

    private suspend fun apply(policy: ExistingWorkPolicy) {
        val workManager = WorkManager.getInstance(context)
        val settings = preferences.reminder.first()
        if (!settings.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = ReminderTime.millisUntilNext(
            now = System.currentTimeMillis(),
            hour = settings.hour,
            minute = settings.minute
        )
        val request = OneTimeWorkRequestBuilder<DailyWordWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val WORK_NAME = "daily_word_reminder_v2"
    }
}
