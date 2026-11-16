package com.vocabulary.app.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits the start of the current local day now, and again at every midnight while collected. */
object DayTicker {

    fun dayStarts(): Flow<Long> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(SpacedRepetition.startOfDayAfter(now, 0))
            val untilMidnight = SpacedRepetition.startOfDayAfter(now, 1) - now
            delay(untilMidnight.coerceAtLeast(1_000L) + 500L)
        }
    }
}
