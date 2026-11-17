package com.vocabulary.app.domain

import com.vocabulary.app.data.seed.WordSeedDto

/** What importing a file would do to the word list. [errors] non-empty means it must not be applied. */
data class MergePlan(
    /** The whole list after the merge: every existing word plus the file's words (the file wins on the same id). */
    val merged: List<WordSeedDto>,
    val added: Int,
    val updated: Int,
    val unchanged: Int,
    val errors: List<String>
)

/**
 * "Cumulative" update: words in the file are added (new id) or replaced (same id).
 * Words that are not in the file are always kept, nothing is deleted.
 */
object WordMerge {

    fun plan(existing: List<WordSeedDto>, incoming: List<WordSeedDto>): MergePlan {
        val errors = ArrayList<String>()
        val byId = LinkedHashMap<Long, WordSeedDto>()
        existing.forEach { byId[it.id] = it }

        // Same word + category + part of speech under two ids would show up twice in the app.
        val contentKey = HashMap<String, Long>()
        existing.forEach { contentKey.putIfAbsent(key(it), it.id) }

        val seenIds = HashSet<Long>()
        var added = 0
        var updated = 0
        var unchanged = 0
        for (w in incoming) {
            val label = "id ${w.id} (${w.word.trim()})"
            if (!seenIds.add(w.id)) {
                errors += "Duplicate $label inside the file."
                continue
            }
            val missing = listOf(
                "word" to w.word,
                "partOfSpeech" to w.partOfSpeech,
                "banglaMeaning" to w.banglaMeaning,
                "englishMeaning" to w.englishMeaning
            ).filter { it.second.isBlank() }.map { it.first }
            if (missing.isNotEmpty()) {
                errors += "$label: ${missing.joinToString()} is empty."
                continue
            }
            val clash = contentKey[key(w)]
            if (clash != null && clash != w.id) {
                errors += "$label is the same word as id $clash. Use the old id to update it."
                continue
            }
            contentKey.putIfAbsent(key(w), w.id)

            val old = byId[w.id]
            when {
                old == null -> added++
                old == w -> unchanged++
                else -> updated++
            }
            byId[w.id] = w
        }
        return MergePlan(byId.values.toList(), added, updated, unchanged, errors)
    }

    private fun key(w: WordSeedDto): String =
        listOf(w.type.name, w.word, w.partOfSpeech).joinToString("|") {
            it.trim().lowercase().replace(Regex("\\s+"), " ")
        }
}
