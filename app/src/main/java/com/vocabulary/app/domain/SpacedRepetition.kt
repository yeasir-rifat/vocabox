package com.vocabulary.app.domain

import com.vocabulary.app.data.local.entity.ProgressEntity
import com.vocabulary.app.data.model.WordStatus
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * SM-2 lite scheduler. Pure functions, no Android dependencies.
 *
 * Correct answer: the interval grows 1 day, 3 days, then previous interval x easeFactor.
 * Wrong answer: the interval resets to 1 day and the ease factor drops.
 * A word is MASTERED once its interval reaches [MASTERED_INTERVAL_DAYS].
 *
 * The next review is placed at the start of a calendar day, so a word that is due
 * "tomorrow" is available for the whole of tomorrow, not from the hour it was studied.
 */
object SpacedRepetition {

    const val DEFAULT_EASE = 2.5f
    const val MIN_EASE = 1.3f
    const val MAX_EASE = 3.0f
    const val MASTERED_INTERVAL_DAYS = 21
    const val MAX_INTERVAL_DAYS = 365

    private const val EASE_STEP_UP = 0.05f
    private const val EASE_STEP_DOWN = 0.2f

    /** Returns the new progress after the user answered a card. [current] is null for a new word. */
    fun review(current: ProgressEntity?, wordId: Long, known: Boolean, now: Long): ProgressEntity {
        val base = current ?: ProgressEntity(wordId = wordId, easeFactor = DEFAULT_EASE)
        return if (known) onCorrect(base, now) else onWrong(base, now)
    }

    private fun onCorrect(p: ProgressEntity, now: Long): ProgressEntity {
        val interval = when {
            p.intervalDays <= 0 -> 1
            p.intervalDays == 1 -> 3
            else -> max(p.intervalDays + 1, (p.intervalDays * p.easeFactor).roundToInt())
        }.coerceAtMost(MAX_INTERVAL_DAYS)

        return p.copy(
            status = if (interval >= MASTERED_INTERVAL_DAYS) WordStatus.MASTERED else WordStatus.LEARNING,
            easeFactor = min(p.easeFactor + EASE_STEP_UP, MAX_EASE),
            intervalDays = interval,
            nextReviewAt = startOfDayAfter(now, interval),
            correctCount = p.correctCount + 1,
            lastReviewedAt = now
        )
    }

    private fun onWrong(p: ProgressEntity, now: Long): ProgressEntity =
        p.copy(
            status = WordStatus.LEARNING,
            easeFactor = max(p.easeFactor - EASE_STEP_DOWN, MIN_EASE),
            intervalDays = 1,
            nextReviewAt = startOfDayAfter(now, 1),
            wrongCount = p.wrongCount + 1,
            lastReviewedAt = now
        )

    /** Local midnight, [days] calendar days after the day that contains [now]. */
    internal fun startOfDayAfter(now: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }
}
