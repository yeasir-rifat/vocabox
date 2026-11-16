package com.vocabulary.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vocabulary.app.data.local.dao.ProgressDao
import com.vocabulary.app.data.local.dao.WordDao
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.domain.DailyWordPicker
import com.vocabulary.app.domain.DayUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Posts today's word as a notification. Scheduled once a day by [ReminderScheduler]. */
@HiltWorker
class DailyWordWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val wordDao: WordDao,
    private val progressDao: ProgressDao,
    private val preferences: AppPreferences,
    private val notifier: DailyWordNotifier,
    private val scheduler: ReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!preferences.reminder.first().enabled) return Result.success()
        try {
            val today = DayUtils.epochDay(System.currentTimeMillis())
            val words = wordDao.getAll()
            // The word is pinned for the day, so the notification and Home always agree.
            val word = preferences.pinnedDailyWord(today)?.let { id -> words.firstOrNull { it.id == id } }
                ?: DailyWordPicker.pick(
                    words = words,
                    progress = progressDao.getAll().associateBy { it.wordId },
                    today = today
                )?.also { preferences.pinDailyWord(today, it.id) }
            if (word != null) notifier.show(word)
        } finally {
            // Plan tomorrow's run, even if today's failed.
            scheduler.scheduleNext()
        }
        return Result.success()
    }
}
