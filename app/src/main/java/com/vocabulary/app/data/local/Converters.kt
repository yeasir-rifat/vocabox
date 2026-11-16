package com.vocabulary.app.data.local

import androidx.room.TypeConverter
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.WordStatus
import com.vocabulary.app.data.model.WordType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val listSerializer = ListSerializer(String.serializer())

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        Json.encodeToString(listSerializer, value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        runCatching { Json.decodeFromString(listSerializer, value) }
            .getOrDefault(emptyList())

    @TypeConverter
    fun fromWordType(value: WordType): String = value.name

    @TypeConverter
    fun toWordType(value: String): WordType =
        runCatching { WordType.valueOf(value) }.getOrDefault(WordType.WORD)

    @TypeConverter
    fun fromWordStatus(value: WordStatus): String = value.name

    @TypeConverter
    fun toWordStatus(value: String): WordStatus =
        runCatching { WordStatus.valueOf(value) }.getOrDefault(WordStatus.NEW)

    @TypeConverter
    fun fromQuizMode(value: QuizMode): String = value.name

    @TypeConverter
    fun toQuizMode(value: String): QuizMode =
        runCatching { QuizMode.valueOf(value) }.getOrDefault(QuizMode.MEANING)
}
