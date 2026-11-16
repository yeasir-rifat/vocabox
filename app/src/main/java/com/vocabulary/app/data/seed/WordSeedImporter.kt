package com.vocabulary.app.data.seed

import android.content.Context
import android.util.Log
import com.vocabulary.app.data.local.dao.WordDao
import com.vocabulary.app.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Where the word import stands. The UI shows a loader or a retry button until it is [Ready]. */
enum class SeedStatus { LOADING, READY, FAILED }

/**
 * Imports the bundled word list into Room.
 * Runs on first launch and again whenever the "version" field in words.json is increased.
 * Existing words are updated in place, so user progress, bookmarks and mistakes are preserved.
 * Words that are no longer in words.json are removed (with their progress).
 */
@Singleton
class WordSeedImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordDao: WordDao,
    private val preferences: AppPreferences,
    private val json: Json
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow(SeedStatus.LOADING)
    val status: StateFlow<SeedStatus> = _status.asStateFlow()

    suspend fun importIfNeeded(): Unit = mutex.withLock {
        withContext(Dispatchers.IO) {
            _status.value = SeedStatus.LOADING
            try {
                val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
                val seed = json.decodeFromString<WordSeedFile>(text)

                val importedVersion = preferences.seedVersion.first()
                val isEmpty = wordDao.count() == 0
                if (!isEmpty && seed.version <= importedVersion) {
                    Log.d(TAG, "Seed is up to date (version ${seed.version})")
                    _status.value = SeedStatus.READY
                    return@withContext
                }

                wordDao.upsertAll(seed.words.map { it.toEntity() })

                val keep = seed.words.mapTo(HashSet()) { it.id }
                val stale = wordDao.getAllIds().filter { it !in keep }
                // Chunks stay below SQLite's 999 variable limit on older Android versions.
                stale.chunked(500).forEach { wordDao.deleteByIds(it) }

                preferences.setSeedVersion(seed.version)
                Log.i(TAG, "Imported ${seed.words.size} words (version ${seed.version}), removed ${stale.size}")
                _status.value = SeedStatus.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import $ASSET_NAME", e)
                // Old data is still usable; only an empty database is a real failure.
                _status.value = if (wordDao.count() > 0) SeedStatus.READY else SeedStatus.FAILED
            }
        }
    }

    private companion object {
        const val TAG = "WordSeedImporter"
        const val ASSET_NAME = "words.json"
    }
}
