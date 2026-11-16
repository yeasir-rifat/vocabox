package com.vocabulary.app.worker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vocabulary.app.MainActivity
import com.vocabulary.app.R
import com.vocabulary.app.data.local.entity.WordEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Builds and posts the "word of the day" notification. */
@Singleton
class DailyWordNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Returns false when notifications are blocked (permission denied or turned off). */
    @SuppressLint("MissingPermission") // areNotificationsEnabled() covers the API 33 permission
    fun show(word: WordEntity): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel(manager)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_WORD_ID, word.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = listOf(word.partOfSpeech, word.banglaMeaning)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val details = buildString {
            append(word.banglaMeaning)
            if (word.englishMeaning.isNotBlank()) append("\n").append(word.englishMeaning)
            if (word.example.isNotBlank()) append("\n").append(word.example)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.daily_word_title, word.word))
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun ensureChannel(manager: NotificationManagerCompat) {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(context.getString(R.string.daily_word_channel_name))
            .setDescription(context.getString(R.string.daily_word_channel_description))
            .build()
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "daily_word"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_CODE = 1001
    }
}
