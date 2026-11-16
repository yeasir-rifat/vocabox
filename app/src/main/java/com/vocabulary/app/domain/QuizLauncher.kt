package com.vocabulary.app.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Hand-over point: a screen posts a request, the Quiz tab picks it up once. */
@Singleton
class QuizLauncher @Inject constructor() {
    private val _pending = MutableStateFlow<QuizGroupRequest?>(null)
    val pending: StateFlow<QuizGroupRequest?> = _pending.asStateFlow()

    fun request(request: QuizGroupRequest) {
        _pending.value = request
    }

    fun consume() {
        _pending.value = null
    }
}
