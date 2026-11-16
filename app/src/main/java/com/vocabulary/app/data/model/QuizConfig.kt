package com.vocabulary.app.data.model

import com.vocabulary.app.domain.QuizLimits

/** Last quiz setup choices. Persisted in AppPreferences. */
data class QuizConfig(
    val source: QuizSource = QuizSource.ALL,
    val modes: Set<QuizMode> = setOf(QuizMode.MEANING),
    val questionCount: Int = QuizLimits.DEFAULT_COUNT,
    /** Minutes for List Quiz, 0 = no limit. */
    val listTimeMinutes: Int = QuizLimits.DEFAULT_MINUTES,
    val viewStyle: QuizViewStyle = QuizViewStyle.CARD
)
