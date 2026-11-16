package com.vocabulary.app.domain

import com.vocabulary.app.data.model.QuizMode

/** Quiz limits and small pure helpers, kept free of Android so they can be unit tested. */
object QuizLimits {
    const val MIN_COUNT = 1
    const val MAX_COUNT = 100
    const val DEFAULT_COUNT = 10

    /** 0 means "no time limit". */
    const val MIN_MINUTES = 0
    const val MAX_MINUTES = 180
    const val DEFAULT_MINUTES = 5

    /** Seconds per question in Step Quiz. Change here to change the timer everywhere. */
    const val STEP_SECONDS_PER_QUESTION = 20

    fun clampCount(value: Int): Int = value.coerceIn(MIN_COUNT, MAX_COUNT)
    fun clampMinutes(value: Int): Int = value.coerceIn(MIN_MINUTES, MAX_MINUTES)

    /** Number of questions a quiz will really have: the request, capped by what is available. */
    fun effectiveCount(requested: Int, available: Int): Int =
        minOf(clampCount(requested), available.coerceAtLeast(0))
}

/** Scoring for List Quiz. */
object QuizScoring {
    /** [answers] maps question index to the chosen option index. Missing entries count as wrong. */
    fun correctCount(questions: List<QuizQuestion>, answers: Map<Int, Int>): Int =
        questions.indices.count { i -> answers[i] == questions[i].correctIndex }
}

/** Word ids each mode can build a question for, for one source. */
data class QuizAvailability(
    val ids: Map<QuizMode, Set<Long>> = emptyMap(),
    val loaded: Boolean = false
) {
    fun count(mode: QuizMode): Int = ids[mode]?.size ?: 0

    /** Distinct words across [modes]. A word can feed several modes but is asked only once. */
    fun totalFor(modes: Collection<QuizMode>): Int =
        modes.flatMapTo(HashSet()) { ids[it].orEmpty() }.size
}
