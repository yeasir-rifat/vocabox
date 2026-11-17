package com.vocabulary.app.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.vocabulary.app.BuildConfig
import com.vocabulary.app.data.auth.AuthRepository
import com.vocabulary.app.data.model.WordType
import com.vocabulary.app.data.prefs.AppPreferences
import com.vocabulary.app.data.seed.WordSeedDto
import com.vocabulary.app.data.seed.WordSeedFile
import com.vocabulary.app.data.seed.WordSeedImporter
import com.vocabulary.app.domain.MergePlan
import com.vocabulary.app.domain.WordMerge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Which Google accounts may open the admin tools (build-time list; Firestore rules are the real lock). */
object AdminConfig {
    private val emails: Set<String> = BuildConfig.ADMIN_EMAILS
        .split(',', ';', ' ')
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .toSet()

    val isConfigured: Boolean get() = emails.isNotEmpty()
    fun isAdmin(email: String?): Boolean = email != null && email.trim().lowercase() in emails
}

/** The result of reading a file, before anything is written. */
data class ImportPreview(
    val plan: MergePlan,
    val version: Int,
    /** Pack ids currently in Firestore, so packs that disappear after the merge can be removed. */
    val oldPackIds: List<String>
)

/** A message that is safe to show to the person. */
class AdminException(message: String) : Exception(message)

/**
 * Admin-only cumulative word update (Settings > Admin login).
 *
 * Import reads a words.json, merges it into the current list by id (new ids are added, same ids are
 * replaced, nothing is deleted), uploads the merged list to Cloud Firestore in the same pack layout as
 * scripts/upload-words.mjs, and then saves it into the local Room database. Because Firestore is updated
 * too, a later "Sync words" gives the same list instead of undoing the import.
 */
@Singleton
class WordAdmin @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthRepository,
    private val preferences: AppPreferences,
    private val importer: WordSeedImporter,
    private val json: Json
) {
    private val wordsSerializer = ListSerializer(WordSeedDto.serializer())

    /** Reads [text] and works out what a merge would change. Writes nothing. */
    suspend fun preview(text: String): ImportPreview = guard {
        requireAdmin()
        val (fileVersion, incoming) = withContext(Dispatchers.Default) { parseIncoming(text) }
        if (incoming.isEmpty()) throw AdminException("The file has no words.")
        val (base, oldPacks) = loadCurrent()
        val plan = withContext(Dispatchers.Default) { WordMerge.plan(base.words, incoming) }
        ImportPreview(plan, maxOf(base.version, fileVersion), oldPacks)
    }

    /** Uploads the merged list to Firestore, then saves it in the local database. Returns the word count. */
    suspend fun apply(preview: ImportPreview): Int = guard {
        requireAdmin()
        val plan = preview.plan
        if (plan.errors.isNotEmpty()) throw AdminException("Fix the problems in the file first.")
        val packs = withContext(Dispatchers.Default) { buildPacks(plan.merged) }
        val stamp = System.currentTimeMillis()

        withTimeout(WRITE_TIMEOUT_MS) {
            val db = db()
            // Packs first, meta last: other phones only read the packs listed in meta, so they never see half an upload.
            val batch = db.batch()
            packs.forEach { p ->
                batch.set(
                    db.collection(RemoteWordSync.PACKS).document(p.id),
                    mapOf("json" to p.json, "count" to p.words.size, "type" to p.type.name)
                )
            }
            batch.commit().await()
            db.collection(RemoteWordSync.META).document(RemoteWordSync.META_DOC).set(
                mapOf(
                    "version" to preview.version,
                    "stamp" to stamp,
                    "total" to plan.merged.size,
                    "packs" to packs.map { it.id }
                )
            ).await()

            val keep = packs.mapTo(HashSet()) { it.id }
            val stale = preview.oldPackIds.filter { it !in keep }
            if (stale.isNotEmpty()) {
                val cleanup = db.batch()
                stale.forEach { cleanup.delete(db.collection(RemoteWordSync.PACKS).document(it)) }
                try { cleanup.commit().await() } catch (e: FirebaseFirestoreException) { /* leftovers are harmless */ }
            }
        }

        val count = importer.applyRemote(WordSeedFile(version = preview.version, words = plan.merged))
        preferences.setRemoteSyncResult(stamp, System.currentTimeMillis())
        count
    }

    /** The whole current list as a words.json text, for backup or editing on a computer. */
    suspend fun exportJson(): String = guard {
        requireAdmin()
        val (base, _) = loadCurrent()
        withContext(Dispatchers.Default) {
            val pretty = Json { prettyPrint = true; prettyPrintIndent = "  " }
            pretty.encodeToString(WordSeedFile.serializer(), base)
        }
    }

    // ---- internals -------------------------------------------------------------------------

    private fun requireAdmin() {
        if (!auth.isConfigured || !auth.isSignedIn) throw AdminException("Sign in first.")
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (!AdminConfig.isAdmin(email)) throw AdminException("This account is not an admin.")
    }

    private fun db() = FirebaseFirestore.getInstance(FirebaseApp.getInstance())

    /** The list as it is in Firestore now; the bundled words.json when nothing was uploaded yet. */
    private suspend fun loadCurrent(): Pair<WordSeedFile, List<String>> = withTimeout(READ_TIMEOUT_MS) {
        val db = db()
        val meta = db.collection(RemoteWordSync.META).document(RemoteWordSync.META_DOC).get(Source.SERVER).await()
        if (!meta.exists()) {
            val text = withContext(Dispatchers.IO) {
                context.assets.open("words.json").bufferedReader().use { it.readText() }
            }
            return@withTimeout json.decodeFromString<WordSeedFile>(text) to emptyList()
        }
        val packIds = (meta.get("packs") as? List<*>).orEmpty().filterIsInstance<String>()
        val words = ArrayList<WordSeedDto>()
        for (id in packIds) {
            val pack = db.collection(RemoteWordSync.PACKS).document(id).get(Source.SERVER).await()
            val text = pack.getString("json") ?: throw AdminException("Pack \"$id\" is missing in Firestore.")
            words += json.decodeFromString(wordsSerializer, text)
        }
        WordSeedFile((meta.getLong("version") ?: 0L).toInt(), words) to packIds
    }

    /** Accepts {"version": 8, "words": [...]} or just a [...] list of words. */
    private fun parseIncoming(text: String): Pair<Int, List<WordSeedDto>> {
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw AdminException("This is not a valid JSON file.")
        }
        val (version, array) = when (root) {
            is JsonArray -> 0 to root
            is JsonObject -> {
                val words = root["words"] as? JsonArray
                    ?: throw AdminException("The file needs a \"words\" list.")
                ((root["version"] as? JsonPrimitive)?.intOrNull ?: 0) to words
            }
            else -> throw AdminException("The file needs a \"words\" list.")
        }
        val words = try {
            json.decodeFromJsonElement(wordsSerializer, array)
        } catch (e: SerializationException) {
            throw AdminException("A word is in the wrong format: ${e.message?.lineSequence()?.firstOrNull().orEmpty()}")
        } catch (e: IllegalArgumentException) {
            throw AdminException("A word is in the wrong format: ${e.message?.lineSequence()?.firstOrNull().orEmpty()}")
        }
        return version to words
    }

    private class Pack(val id: String, val type: WordType, val words: List<WordSeedDto>, val json: String)

    /** One pack per category, split when large (same limits as scripts/upload-words.mjs; Firestore allows 1 MiB). */
    private fun buildPacks(words: List<WordSeedDto>): List<Pack> {
        val packs = ArrayList<Pack>()
        words.groupBy { it.type }.forEach { (type, list) ->
            val base = type.name.lowercase()
            var current = ArrayList<WordSeedDto>()
            var bytes = 2
            var n = 1
            fun flush() {
                if (current.isNotEmpty()) {
                    packs += Pack("${base}_${n++}", type, current, json.encodeToString(wordsSerializer, current))
                }
                current = ArrayList()
                bytes = 2
            }
            for (w in list) {
                val size = json.encodeToString(WordSeedDto.serializer(), w).toByteArray(Charsets.UTF_8).size + 1
                if (current.size >= MAX_WORDS_PER_PACK || bytes + size > MAX_BYTES_PER_PACK) flush()
                current += w
                bytes += size
            }
            flush()
        }
        return packs
    }

    private suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: TimeoutCancellationException) {
        throw AdminException("Could not reach Firebase. Check the internet and try again.")
    } catch (e: CancellationException) {
        throw e
    } catch (e: AdminException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        throw AdminException(
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Firestore refused the change. Add this account to the admin list in firestore.rules and publish it."
                else -> "Could not reach Firebase. Check the internet and try again."
            }
        )
    } catch (e: SerializationException) {
        throw AdminException("The word list in Firestore is not in the expected format.")
    } catch (e: Exception) {
        throw AdminException("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
    }

    private companion object {
        const val MAX_WORDS_PER_PACK = 300
        const val MAX_BYTES_PER_PACK = 700_000
        const val READ_TIMEOUT_MS = 30_000L
        const val WRITE_TIMEOUT_MS = 45_000L
    }
}
