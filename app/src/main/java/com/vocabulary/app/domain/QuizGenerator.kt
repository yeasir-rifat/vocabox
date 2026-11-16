package com.vocabulary.app.domain

import com.vocabulary.app.ui.components.shortPartOfSpeech
import com.vocabulary.app.ui.components.exampleLines
import com.vocabulary.app.data.local.entity.WordEntity
import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.WordType
import kotlin.random.Random

/** One multiple-choice question. [options] always has [QuizGenerator.OPTION_COUNT] distinct items. */
data class QuizQuestion(
    val wordId: Long,
    val mode: QuizMode,
    val instruction: String,
    val prompt: String,
    val hint: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
) {
    val correctAnswer: String get() = options[correctIndex]
}

/** Builds quiz questions. Pure Kotlin, so it can be unit tested without Android. */
object QuizGenerator {

    const val OPTION_COUNT = 4

    /** Wrong options for preposition questions. Only prepositions that are rarely interchangeable. */
    private val COMMON_PREPOSITIONS = listOf(
        "of", "in", "on", "at", "for", "with", "by", "to", "from", "into", "over", "against"
    )

    /** Prepositions that can often replace each other. Never offered next to the right answer. */
    private val CONFUSABLE: List<Set<String>> = listOf(
        setOf("in", "into", "at"),
        setOf("on", "over", "upon"),
        setOf("to", "into"),
        setOf("of", "from"),
        setOf("by", "with")
    )

    private fun confusableWith(preposition: String): Set<String> =
        CONFUSABLE.filter { preposition in it }.flatten().toSet() - preposition

    /**
     * Question stems in the style of BCS, job-recruitment and admission tests: short, formal, often
     * ending with a dash. {w} is the word, meaning or sentence. Quotes are the typographic \u2018 \u2019.
     */
    private val TEMPLATES: Map<QuizMode, List<String>> = mapOf(
        QuizMode.MEANING to listOf(
            "Choose the correct Bangla meaning of \u2018{w}\u2019.",
            "Bangla meaning of \u2018{w}\u2019 is \u2014",
            "Select the right Bangla equivalent of \u2018{w}\u2019."
        ),
        QuizMode.SYNONYM to listOf(
            "Choose the correct synonym of \u2018{w}\u2019.",
            "Synonym of \u2018{w}\u2019 is \u2014",
            "Select the word nearest in meaning to \u2018{w}\u2019."
        ),
        QuizMode.ANTONYM to listOf(
            "Choose the correct antonym of \u2018{w}\u2019.",
            "Antonym of \u2018{w}\u2019 is \u2014",
            "Select the word opposite in meaning to \u2018{w}\u2019."
        ),
        QuizMode.ONE_WORD_SUB to listOf(
            "Choose the appropriate one word for \u2018{w}\u2019.",
            "One word for \u2018{w}\u2019 is \u2014",
            "Select the right word for the expression \u2018{w}\u2019."
        ),
        QuizMode.IDIOM to listOf(
            "Choose the correct meaning of the idiom \u2018{w}\u2019.",
            "The idiom \u2018{w}\u2019 means \u2014",
            "Select the right meaning of the phrase \u2018{w}\u2019."
        ),
        QuizMode.GROUP_VERB to listOf(
            "Choose the correct meaning of the group verb \u2018{w}\u2019.",
            "The group verb \u2018{w}\u2019 means \u2014",
            "Select the right meaning of \u2018{w}\u2019."
        ),
        QuizMode.PROVERB to listOf(
            "Choose the correct meaning of the proverb \u2018{w}\u2019.",
            "The proverb \u2018{w}\u2019 means \u2014",
            "Select the appropriate meaning of the proverb \u2018{w}\u2019."
        ),
        QuizMode.PREPOSITION to listOf(
            "Fill in the blank with the appropriate preposition.\n{w}",
            "Choose the correct preposition to fill in the blank.\n{w}",
            "Select the right preposition.\n{w}"
        )
    )

    private fun phrase(mode: QuizMode, subject: String, random: Random): String =
        TEMPLATES.getValue(mode).random(random).replace("{w}", subject)

    private class Pools(words: List<WordEntity>) {
        val plain = words.filter { it.type == WordType.WORD }
        val oneWord = words.filter { it.type == WordType.ONE_WORD_SUB }
        val idioms = words.filter { it.type == WordType.IDIOM }
        val groupVerbs = words.filter { it.type == WordType.GROUP_VERB }
        val prepositions = words.filter { it.type == WordType.PREPOSITION }
        val proverbs = words.filter { it.type == WordType.PROVERB }
    }

    /**
     * Word ids each mode can build a question for. [targets] are the words that may be asked
     * (the chosen source); [pool] is the full word list used for wrong options, so a small
     * source such as Favorites still gets four distinct options.
     */
    fun buildableIds(
        targets: List<WordEntity>,
        pool: List<WordEntity> = targets
    ): Map<QuizMode, Set<Long>> {
        val targetPools = Pools(targets)
        val optionPools = Pools(pool)
        return QuizMode.entries.associateWith { mode ->
            candidates(mode, targetPools)
                .filter { build(mode, it, optionPools, Random(0)) != null }
                .mapTo(LinkedHashSet()) { it.id }
        }
    }

    /** How many questions each mode can produce from [targets]. */
    fun availability(targets: List<WordEntity>, pool: List<WordEntity> = targets): Map<QuizMode, Int> =
        buildableIds(targets, pool).mapValues { it.value.size }

    fun generate(
        mode: QuizMode,
        words: List<WordEntity>,
        count: Int,
        random: Random = Random.Default,
        pool: List<WordEntity> = words
    ): List<QuizQuestion> {
        val optionPools = Pools(pool)
        return candidates(mode, Pools(words))
            .shuffled(random)
            .asSequence()
            .mapNotNull { build(mode, it, optionPools, random) }
            .take(count)
            .toList()
    }

    /**
     * Builds a mixed quiz of up to [total] questions. Questions are dealt round-robin over
     * [modes] (in QuizMode order), so each mode gets an equal share until one runs out. A word
     * is asked at most once. The final order is shuffled.
     */
    fun generateMixed(
        modes: Collection<QuizMode>,
        targets: List<WordEntity>,
        total: Int,
        random: Random = Random.Default,
        pool: List<WordEntity> = targets
    ): List<QuizQuestion> {
        if (total <= 0 || modes.isEmpty()) return emptyList()
        val targetPools = Pools(targets)
        val optionPools = Pools(pool)
        val ordered = QuizMode.entries.filter { it in modes }
        val queues = ordered.associateWith { ArrayDeque(candidates(it, targetPools).shuffled(random)) }
        val usedWords = HashSet<Long>()
        val result = ArrayList<QuizQuestion>()

        var progress = true
        while (result.size < total && progress) {
            progress = false
            for (mode in ordered) {
                if (result.size >= total) break
                val queue = queues.getValue(mode)
                while (queue.isNotEmpty()) {
                    val word = queue.removeFirst()
                    if (word.id in usedWords) continue
                    val question = build(mode, word, optionPools, random) ?: continue
                    usedWords += word.id
                    result += question
                    progress = true
                    break
                }
            }
        }
        return result.shuffled(random)
    }

    private fun candidates(mode: QuizMode, pools: Pools): List<WordEntity> =
        baseCandidates(mode, pools).filter { ItemQuestions.allows(it, mode) }

    private fun baseCandidates(mode: QuizMode, pools: Pools): List<WordEntity> = when (mode) {
        QuizMode.MEANING -> pools.plain
        QuizMode.SYNONYM -> pools.plain.filter { it.synonyms.isNotEmpty() }
        QuizMode.ANTONYM -> pools.plain.filter { it.antonyms.isNotEmpty() }
        QuizMode.ONE_WORD_SUB -> pools.oneWord
        QuizMode.IDIOM -> pools.idioms
        QuizMode.GROUP_VERB -> pools.groupVerbs
        QuizMode.PREPOSITION -> pools.prepositions
        QuizMode.PROVERB -> pools.proverbs
    }

    private fun build(mode: QuizMode, word: WordEntity, pools: Pools, random: Random): QuizQuestion? {
        val subject: String
        val hint: String
        val correct: String
        val wrong: List<String>

        when (mode) {
            QuizMode.MEANING -> {
                subject = word.word
                hint = "" // the part of speech is not shown in questions
                correct = word.banglaMeaning
                wrong = distractors(word, pools.plain, random, listOf(correct, word.word)) { it.banglaMeaning }
            }
            QuizMode.SYNONYM -> {
                subject = word.word
                hint = "" // the part of speech is not shown in questions
                correct = word.synonyms.random(random)
                wrong = distractors(
                    word, pools.plain, random,
                    exclude = word.synonyms + word.word,
                    priority = word.antonyms
                ) { it.synonyms.randomOrNull(random) }
            }
            QuizMode.ANTONYM -> {
                subject = word.word
                hint = "" // the part of speech is not shown in questions
                correct = word.antonyms.random(random)
                wrong = distractors(
                    word, pools.plain, random,
                    exclude = word.antonyms + word.word,
                    priority = word.synonyms
                ) { it.antonyms.randomOrNull(random) }
            }
            QuizMode.ONE_WORD_SUB -> {
                subject = word.englishMeaning
                hint = ""
                correct = word.word
                wrong = distractors(word, pools.oneWord, random, listOf(correct)) { it.word }
            }
            QuizMode.IDIOM -> {
                subject = word.word
                hint = ""
                correct = word.englishMeaning
                wrong = distractors(word, pools.idioms, random, listOf(correct)) { it.englishMeaning }
            }
            QuizMode.GROUP_VERB -> {
                subject = word.word
                hint = ""
                correct = word.englishMeaning
                wrong = distractors(word, pools.groupVerbs, random, listOf(correct)) { it.englishMeaning }
            }
            QuizMode.PROVERB -> {
                subject = word.word
                hint = ""
                correct = word.englishMeaning
                wrong = distractors(word, pools.proverbs, random, listOf(correct)) { it.englishMeaning }
            }
            QuizMode.PREPOSITION -> {
                val blank = prepositionBlank(word) ?: return null
                subject = blank.sentence
                hint = ""
                correct = blank.preposition
                val unfair = confusableWith(correct)
                wrong = COMMON_PREPOSITIONS.filter { it != correct && it !in unfair }.shuffled(random).take(OPTION_COUNT - 1)
            }
        }

        if (wrong.size < OPTION_COUNT - 1) return null

        var finalPrompt = phrase(mode, subject, random)
        var finalCorrect = correct
        var finalWrong = wrong

        // The item may bring its own question sentence and options (words.json "questions").
        val custom = ItemQuestions.forMode(word, mode)
            .filter { it.text.isNotBlank() || it.options.isNotEmpty() }
            .randomOrNull(random)
        if (custom != null) {
            if (custom.text.isNotBlank()) {
                val blank = if (mode == QuizMode.PREPOSITION) subject else null
                var text = ItemQuestions.resolve(custom.text, word, blank)
                if (blank != null && !custom.text.contains("{blank}")) text += "\n$blank"
                finalPrompt = text
            }
            val opts = custom.options.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
            val answer = opts.firstOrNull { it.equals(custom.answer.trim(), ignoreCase = true) }
            if (opts.size == OPTION_COUNT && answer != null) {
                finalCorrect = answer
                finalWrong = opts - answer
            }
        }

        val options = (finalWrong + finalCorrect).shuffled(random)
        return QuizQuestion(
            wordId = word.id,
            mode = mode,
            instruction = "",
            prompt = finalPrompt,
            hint = hint,
            options = options,
            correctIndex = options.indexOf(finalCorrect),
            explanation = explanationFor(word)
        )
    }

    private class PrepositionBlank(val sentence: String, val preposition: String)

    /**
     * Turns a preposition entry into a fill-in-the-blank sentence.
     * [WordEntity.word] is the phrase ("abide by") and [WordEntity.example] must contain that exact
     * phrase; the last word of the phrase is replaced by a blank. Returns null when it cannot.
     */
    private fun prepositionBlank(word: WordEntity): PrepositionBlank? {
        val parts = word.word.trim().split(Regex("\\s+"))
        if (parts.size < 2) return null
        val preposition = parts.last().lowercase()
        val head = parts.dropLast(1).joinToString(" ")
        val pattern = Regex(
            "\\b" + Regex.escape(head) + "\\s+" + Regex.escape(parts.last()) + "\\b",
            RegexOption.IGNORE_CASE
        )
        val example = word.exampleLines().firstOrNull { pattern.containsMatchIn(it) } ?: return null
        val match = pattern.find(example) ?: return null
        val sentence = example.replaceRange(match.range, match.value.substring(0, head.length) + " ____")
        return PrepositionBlank(sentence, preposition)
    }

    /**
     * The explanation shown after an answer. **Double asterisks** mark bold text:
     *
     *     **Frugal** (adj.) = মিতব্যয়ী, স্বল্পব্যয়ী
     *     (Careful about spending money)
     *     **Synonyms:** thrifty (সঞ্চয়ী), economical (মিতব্যয়ী)
     *     **Antonyms:** extravagant (অমিতব্যয়ী)
     *     **Examples**
     *     \u2022 Living a **frugal** life helped him save.
     */
    private fun explanationFor(word: WordEntity): String {
        val lines = ArrayList<String>()
        val pos = word.partOfSpeech.shortPartOfSpeech()
        val bangla = word.banglaMeaning.split(';', ',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
        lines += buildString {
            append("**").append(word.word.replaceFirstChar { it.uppercase() }).append("**")
            if (pos.isNotEmpty()) append(" ($pos)")
            if (bangla.isNotEmpty()) append(" = ").append(bangla)
        }
        if (word.englishMeaning.isNotBlank()) {
            lines += "(${word.englishMeaning.trim().replaceFirstChar { it.uppercase() }})"
        }
        withBangla(word.synonyms, word.synonymsBn).takeIf { it.isNotEmpty() }
            ?.let { lines += "**Synonyms:** $it" }
        withBangla(word.antonyms, word.antonymsBn).takeIf { it.isNotEmpty() }
            ?.let { lines += "**Antonyms:** $it" }
        val examples = word.exampleLines()
        if (examples.isNotEmpty()) {
            lines += "**Examples**"
            examples.forEach { lines += "\u2022 ${boldWord(it, word.word)}" }
        }
        return lines.joinToString("\n")
    }

    /** Marks the given word (also with an ending, like "abandoned") in an example as bold. */
    private fun boldWord(example: String, word: String): String {
        val target = word.trim()
        if (target.isEmpty()) return example
        val m = Regex("\\b" + Regex.escape(target) + "\\w*", RegexOption.IGNORE_CASE).find(example)
            ?: Regex(Regex.escape(target), RegexOption.IGNORE_CASE).find(example)
            ?: return example
        return example.replaceRange(m.range, "**" + m.value + "**")
    }

    private fun withBangla(items: List<String>, bangla: List<String>): String =
        items.mapIndexed { i, e ->
            val bn = bangla.getOrNull(i)?.takeIf { it.isNotBlank() }
            if (bn != null) "$e ($bn)" else e
        }.joinToString(", ")

    /**
     * Picks wrong options. Words that share the part of speech or topic with [target] come first,
     * [priority] texts are tried before that, and duplicates of [exclude] are never used.
     */
    private fun distractors(
        target: WordEntity,
        pool: List<WordEntity>,
        random: Random,
        exclude: Collection<String>,
        priority: List<String> = emptyList(),
        pick: (WordEntity) -> String?
    ): List<String> {
        val seen = exclude.mapTo(HashSet()) { it.trim().lowercase() }
        val result = ArrayList<String>()

        fun tryAdd(text: String?) {
            if (text != null && text.isNotBlank() && result.size < OPTION_COUNT - 1 &&
                seen.add(text.trim().lowercase())
            ) {
                result.add(text)
            }
        }

        priority.shuffled(random).take(1).forEach { tryAdd(it) }

        pool.filter { it.id != target.id }
            .shuffled(random)
            .sortedByDescending { other ->
                (if (other.partOfSpeech == target.partOfSpeech) 2 else 0) +
                    (if (other.topic == target.topic) 1 else 0)
            }
            .forEach { tryAdd(pick(it)) }

        return result
    }
}
