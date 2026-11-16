package com.vocabulary.app.data.local.entity

import androidx.room.Embedded

/** A missed word together with how many times it was missed. */
data class MistakeWithWord(
    @Embedded val word: WordEntity,
    val count: Int
)
