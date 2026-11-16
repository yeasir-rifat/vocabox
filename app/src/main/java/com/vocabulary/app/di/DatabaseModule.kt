package com.vocabulary.app.di

import android.content.Context
import androidx.room.Room
import com.vocabulary.app.data.local.VocabularyDatabase
import com.vocabulary.app.data.local.dao.ActivityDao
import com.vocabulary.app.data.local.dao.BookmarkDao
import com.vocabulary.app.data.local.dao.MistakeDao
import com.vocabulary.app.data.local.dao.ProgressDao
import com.vocabulary.app.data.local.dao.QuizResultDao
import com.vocabulary.app.data.local.dao.WordDao
import com.vocabulary.app.data.local.dao.WordReadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VocabularyDatabase =
        Room.databaseBuilder(context, VocabularyDatabase::class.java, VocabularyDatabase.NAME)
            .addMigrations(
                VocabularyDatabase.MIGRATION_1_2,
                VocabularyDatabase.MIGRATION_2_3,
                VocabularyDatabase.MIGRATION_3_4,
                VocabularyDatabase.MIGRATION_4_5,
                VocabularyDatabase.MIGRATION_5_6
            )
            .build()

    @Provides fun provideWordDao(db: VocabularyDatabase): WordDao = db.wordDao()
    @Provides fun provideProgressDao(db: VocabularyDatabase): ProgressDao = db.progressDao()
    @Provides fun provideBookmarkDao(db: VocabularyDatabase): BookmarkDao = db.bookmarkDao()
    @Provides fun provideMistakeDao(db: VocabularyDatabase): MistakeDao = db.mistakeDao()
    @Provides fun provideQuizResultDao(db: VocabularyDatabase): QuizResultDao = db.quizResultDao()
    @Provides fun provideActivityDao(db: VocabularyDatabase): ActivityDao = db.activityDao()
    @Provides fun provideWordReadDao(db: VocabularyDatabase): WordReadDao = db.wordReadDao()
}
