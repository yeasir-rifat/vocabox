package com.vocabulary.app.ui.components

import com.vocabulary.app.data.model.QuizMode
import com.vocabulary.app.data.model.WordType

/** Plural label, used in filters. */
fun WordType.label(): String = when (this) {
    WordType.WORD -> "Words"
    WordType.IDIOM -> "Idioms & Phrases"
    WordType.ONE_WORD_SUB -> "One-word substitution"
    WordType.GROUP_VERB -> "Group Verbs"
    WordType.PREPOSITION -> "Prepositions"
    WordType.PROVERB -> "Proverbs"
}

/** Singular label, used on a single entry. */
fun WordType.singularLabel(): String = when (this) {
    WordType.WORD -> "Word"
    WordType.IDIOM -> "Idiom"
    WordType.ONE_WORD_SUB -> "One-word substitution"
    WordType.GROUP_VERB -> "Group verb"
    WordType.PREPOSITION -> "Preposition"
    WordType.PROVERB -> "Proverb"
}

/** Short badge text for one entry. */
fun WordType.shortLabel(): String = when (this) {
    WordType.WORD -> "Word"
    WordType.IDIOM -> "Idiom"
    WordType.ONE_WORD_SUB -> "One word"
    WordType.GROUP_VERB -> "Group verb"
    WordType.PREPOSITION -> "Preposition"
    WordType.PROVERB -> "Proverb"
}

/** "1 idiom" / "3 idioms": a count with its type, for summary lines. */
fun WordType.countText(count: Int): String {
    val (one, many) = when (this) {
        WordType.WORD -> "word" to "words"
        WordType.IDIOM -> "idiom" to "idioms"
        WordType.ONE_WORD_SUB -> "one-word" to "one-words"
        WordType.GROUP_VERB -> "group verb" to "group verbs"
        WordType.PREPOSITION -> "preposition" to "prepositions"
        WordType.PROVERB -> "proverb" to "proverbs"
    }
    return "$count ${if (count == 1) one else many}"
}

fun levelLabel(level: Int): String = when (level) {
    1 -> "Easy"
    2 -> "Medium"
    else -> "Hard"
}

fun QuizMode.title(): String = when (this) {
    QuizMode.MEANING -> "Word meaning"
    QuizMode.SYNONYM -> "Synonyms"
    QuizMode.ANTONYM -> "Antonyms"
    QuizMode.IDIOM -> "Idioms & Phrases"
    QuizMode.ONE_WORD_SUB -> "One-word substitution"
    QuizMode.GROUP_VERB -> "Group verbs"
    QuizMode.PREPOSITION -> "Appropriate prepositions"
    QuizMode.PROVERB -> "Proverbs"
}

/** Short names for tight spots such as the quiz mode grid. */
fun QuizMode.shortTitle(): String = when (this) {
    QuizMode.MEANING -> "Meaning"
    QuizMode.SYNONYM -> "Synonyms"
    QuizMode.ANTONYM -> "Antonyms"
    QuizMode.IDIOM -> "Idioms & Phrases"
    QuizMode.ONE_WORD_SUB -> "One-word Sub"
    QuizMode.GROUP_VERB -> "Group Verbs"
    QuizMode.PREPOSITION -> "Appr. Prep."
    QuizMode.PROVERB -> "Proverbs"
}

fun QuizMode.description(): String = when (this) {
    QuizMode.MEANING -> "Pick the Bangla meaning of a word"
    QuizMode.SYNONYM -> "Pick the word with the same meaning"
    QuizMode.ANTONYM -> "Pick the word with the opposite meaning"
    QuizMode.IDIOM -> "Pick the meaning of an idiom or phrase"
    QuizMode.ONE_WORD_SUB -> "Pick one word for a whole phrase"
    QuizMode.GROUP_VERB -> "Pick the meaning of a group verb"
    QuizMode.PREPOSITION -> "Fill the blank with the right preposition"
    QuizMode.PROVERB -> "Pick the meaning of a proverb"
}

/** Short dictionary form of a part of speech for compact lists (noun, pron., verb, adj., adv., prep., conj., int.). */
fun String.shortPartOfSpeech(): String = when (trim().lowercase()) {
    "pronoun" -> "pron."
    "preposition" -> "prep."
    "adjective" -> "adj."
    "adverb" -> "adv."
    "conjunction" -> "conj."
    "interjection" -> "int."
    else -> trim()
}
