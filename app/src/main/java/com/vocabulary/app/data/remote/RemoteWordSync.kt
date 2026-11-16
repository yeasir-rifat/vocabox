package com.vocabulary.app.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.vocabulary.app.data.auth.AuthRepository
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.data.seed.WordSeedDto
import com.vocabulary.app.data.seed.WordSeedFile
import com.vocabulary.app.data.seed.WordSeedImporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object UpToDate : SyncState
    data class Updated(val wordCount: Int) : SyncState
    data class Failed(val message: String) : SyncState
}

/**
 * Keeps the word list in step with the word list you upload to Cloud Firestore (free Spark plan).
 *
 * Firestore layout (written by scripts/upload-words.mjs):
 * - `meta/words`  : { version, stamp, total, packs: ["group_verb_1", "idiom_1", ...] }
 * - `packs/{id}`  : { json: "<JSON array of words>", count, type }, one pack per category (split when large)
 *
 * A sync first reads only `meta/words` (1 read). Only when its `stamp` changed does it read the packs
 * listed there, import the words into Room and remember the stamp. Everything the app shows comes from
 * Room, so the words stay fully available offline; a failed sync changes nothing.
 */
@Singleton
class RemoteWordSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthRepository,
    private val preferences: AppPreferences,
    private val importer: WordSeedImporter,
    private val json: Json
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    /** [force] downloads again even if the list looks unchanged (the "Sync words" button). */
    suspend fun sync(force: Boolean = false) {
        if (!auth.isConfigured || !auth.isSignedIn) return
        if (!mutex.tryLock()) return // a sync is already running
        try {
            _state.value = SyncState.Syncing
            _state.value = run(force)
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun run(force: Boolean): SyncState = try {
        // Fail fast when offline instead of waiting for minutes.
        withTimeout(TIMEOUT_MS) { download(force) }
    } catch (e: TimeoutCancellationException) {
        SyncState.Failed("Could not reach Firebase. Using the words saved on this phone.")
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        SyncState.Failed(
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> "This account is not allowed to read the word list."
                else -> "Could not reach Firebase. Using the words saved on this phone."
            }
        )
    } catch (e: kotlinx.serialization.SerializationException) {
        SyncState.Failed("The uploaded word list is not in the expected format.")
    } catch (e: Exception) {
        SyncState.Failed("Sync failed. Using the words saved on this phone.")
    }

    private suspend fun download(force: Boolean): SyncState {
        val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance())

        // Always ask the server: a cached copy would hide a freshly uploaded list.
        val meta = db.collection(META).document(META_DOC).get(Source.SERVER).await()
        if (!meta.exists()) return SyncState.Failed("No word list found in Firestore yet.")

        val stamp = meta.getLong("stamp") ?: 0L
        if (!force && stamp != 0L && stamp == preferences.remoteWordsStamp.first()) {
            preferences.setRemoteSyncResult(stamp, System.currentTimeMillis())
            return SyncState.UpToDate
        }

        val packIds = (meta.get("packs") as? List<*>).orEmpty().filterIsInstance<String>()
        if (packIds.isEmpty()) return SyncState.Failed("The uploaded word list has no packs.")

        val words = ArrayList<WordSeedDto>()
        for (id in packIds) {
            val pack = db.collection(PACKS).document(id).get(Source.SERVER).await()
            val text = pack.getString("json")
                ?: return SyncState.Failed("Pack \"$id\" is missing. Upload the word list again.")
            words += json.decodeFromString<List<WordSeedDto>>(text)
        }
        if (words.isEmpty()) return SyncState.Failed("The uploaded file has no words.")

        val version = (meta.getLong("version") ?: 0L).toInt()
        val count = importer.applyRemote(WordSeedFile(version = version, words = words))
        preferences.setRemoteSyncResult(stamp, System.currentTimeMillis())
        return SyncState.Updated(count)
    }

    companion object {
        const val META = "meta"
        const val META_DOC = "words"
        const val PACKS = "packs"
        private const val TIMEOUT_MS = 25_000L
    }
}
