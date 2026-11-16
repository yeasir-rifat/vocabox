package com.vocabulary.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vocabulary.app.data.local.dao.ActivityDao
import com.vocabulary.app.data.local.entity.DailyActivityEntity
import com.vocabulary.app.data.local.dao.BookmarkDao
import com.vocabulary.app.data.local.dao.MistakeDao
import com.vocabulary.app.data.local.dao.ProgressDao
import com.vocabulary.app.data.local.dao.QuizResultDao
import com.vocabulary.app.data.local.dao.WordDao
import com.vocabulary.app.data.local.dao.WordReadDao
import com.vocabulary.app.data.local.entity.WordReadEntity
import com.vocabulary.app.data.local.entity.BookmarkEntity
import com.vocabulary.app.data.local.entity.MistakeEntity
import com.vocabulary.app.data.local.entity.ProgressEntity
import com.vocabulary.app.data.local.entity.QuizResultEntity
import com.vocabulary.app.data.local.entity.WordEntity

@Database(
    entities = [
        WordEntity::class,
        ProgressEntity::class,
        BookmarkEntity::class,
        MistakeEntity::class,
        QuizResultEntity::class,
        DailyActivityEntity::class,
        WordReadEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VocabularyDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun progressDao(): ProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun activityDao(): ActivityDao
    abstract fun wordReadDao(): WordReadDao

    companion object {
        const val NAME = "vocabulary.db"

        /** Version 2 adds the daily_activity table used by streaks and stats. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_activity` (" +
                        "`day` INTEGER NOT NULL, " +
                        "`reviewed` INTEGER NOT NULL, " +
                        "`correct` INTEGER NOT NULL, " +
                        "`goal` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`day`))"
                )
            }
        }

        /** Version 3 adds word_reads, which tracks words the user has read (word groups). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `word_reads` (" +
                        "`wordId` INTEGER NOT NULL, " +
                        "`firstReadAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`wordId`), " +
                        "FOREIGN KEY(`wordId`) REFERENCES `words`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
            }
        }

        /** Version 4 adds the Bangla meanings of synonyms and antonyms (filled by the seed import). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `words` ADD COLUMN `synonymsBn` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `words` ADD COLUMN `antonymsBn` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /** Version 5 adds the appendix flag used to put exceptions into the "Appendix" card. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `words` ADD COLUMN `appendix` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Version 6 adds the per-item question list (JSON text, filled by the seed import). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `words` ADD COLUMN `questions` TEXT NOT NULL DEFAULT '[]'")
            }
        }
    }
}
