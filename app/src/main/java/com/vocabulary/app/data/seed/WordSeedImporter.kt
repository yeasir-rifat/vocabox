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
 * Puts the word list into Room.
 *
 * - First launch: imports the bundled assets/words.json, so the app works offline from day one.
 * - Later launches: nothing to do (the check reads only the "version" at the top of the file).
 * - Word list downloaded from Firebase ([applyRemote]): replaces the words in place. After that the
 *   bundled list is never imported again, so it cannot overwrite the downloaded one.
 *
 * Existing words are updated in place, so user progress, bookmarks and mistakes are preserved.
 * Words that are no longer in the list are removed (with their progress).
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
                val importedVersion = preferences.seedVersion.first()
                val remoteActive = preferences.remoteWordsActive.first()
                // Fast path for every normal launch: the words are already in Room, so skip parsing.
                if (importedVersion > 0 && wordDao.count() > 0 &&
                    (remoteActive || peekBundledVersion() <= importedVersion)
                ) {
                    _status.value = SeedStatus.READY
                    return@withContext
                }

                val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
                val seed = json.decodeFromString<WordSeedFile>(text)

                val isEmpty = wordDao.count() == 0
                if (!isEmpty && (remoteActive || seed.version <= importedVersion)) {
                    _status.value = SeedStatus.READY
                    return@withContext
                }

                persist(seed)
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

    /** Imports a word list downloaded from Firebase. Returns how many words it holds. */
    suspend fun applyRemote(seed: WordSeedFile): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            persist(seed)
            preferences.setRemoteWordsActive(true)
            _status.value = SeedStatus.READY
            seed.words.size
        }
    }

    private suspend fun persist(seed: WordSeedFile) {
        wordDao.upsertAll(seed.words.map { it.toEntity() })

        val keep = seed.words.mapTo(HashSet()) { it.id }
        val stale = wordDao.getAllIds().filter { it !in keep }
        // Chunks stay below SQLite's 999 variable limit on older Android versions.
        stale.chunked(500).forEach { wordDao.deleteByIds(it) }

        preferences.setSeedVersion(seed.version)
        Log.i(TAG, "Imported ${seed.words.size} words (version ${seed.version}), removed ${stale.size}")
    }

    /** The "version" field sits at the top of words.json. Int.MAX_VALUE (= parse everything) if unreadable. */
    private fun peekBundledVersion(): Int = try {
        val head = context.assets.open(ASSET_NAME).use { stream ->
            val buffer = ByteArray(256)
            String(buffer, 0, stream.read(buffer).coerceAtLeast(0), Charsets.UTF_8)
        }
        VERSION_REGEX.find(head)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
    } catch (e: Exception) {
        Int.MAX_VALUE
    }

    private companion object {
        val VERSION_REGEX = Regex("\"version\"\\s*:\\s*(\\d+)")
        const val TAG = "WordSeedImporter"
        const val ASSET_NAME = "words.json"
    }
}
