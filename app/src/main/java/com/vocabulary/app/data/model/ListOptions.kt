package com.vocabulary.app.data.model

/** Which words to keep by reading state. */
enum class ReadFilter { ALL, READ, UNREAD }

/** How a tapped word of the Groupwise list opens. */
enum class WordView { DETAILS, POPUP, SHEET }

/** The order of the words on screen. */
enum class WordOrder { A_TO_Z, RANDOM }

/**
 * The list settings shared by Word List, Flashcards and Groupwise.
 *
 * [letters] and [count] are separate from the order on purpose: letters pick which words stay,
 * the order arranges them, and [count] keeps the first N of the arranged list ("Random + 20"
 * means 20 random words out of the filtered ones).
 */
data class ListOptions(
    val read: ReadFilter = ReadFilter.ALL,
    val favouriteOnly: Boolean = false,
    /** Uppercase initials chosen on the letter strip. Empty means every letter. */
    val letters: Set<Char> = emptySet(),
    val order: WordOrder = WordOrder.A_TO_Z,
    /** Seed of the Random order, so the list keeps its order until the user shuffles again. */
    val shuffleSeed: Long = 0L,
    /** How many words to show at most. null means all. */
    val count: Int? = null,
    /** Keep only one kind of entry (word, idiom...). null means every type. */
    val type: WordType? = null,
    /** Keep only one level. null means every level. */
    val level: Int? = null,
    /** Whether the A-Z strip is drawn at the right edge (off by default). Hiding it never changes the filter. */
    val showStrip: Boolean = false,
    /** The same switch for the Flashcard page, where the strip is off by default. */
    val cardsStrip: Boolean = false,
    /** How a word of a Groupwise list opens: its own page, a popup card, or a bottom sheet. */
    val wordView: WordView = WordView.POPUP
) {
    /** True when anything differs from the defaults. The seed, the strip switch and the word view do not count. */
    val isActive: Boolean
        get() = read != ReadFilter.ALL ||
            favouriteOnly ||
            letters.isNotEmpty() ||
            order != WordOrder.A_TO_Z ||
            count != null ||
            type != null ||
            level != null
}
