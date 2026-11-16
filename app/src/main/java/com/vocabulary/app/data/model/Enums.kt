package com.vocabulary.app.data.model

/** Kind of vocabulary entry. */
enum class WordType { WORD, IDIOM, ONE_WORD_SUB, GROUP_VERB, PREPOSITION, PROVERB }

/** Learning state of a word for the current user. */
enum class WordStatus { NEW, LEARNING, MASTERED }

/** Supported quiz modes. */
enum class QuizMode { MEANING, SYNONYM, ANTONYM, IDIOM, ONE_WORD_SUB, GROUP_VERB, PREPOSITION, PROVERB }

/** App theme choice. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where a flashcard session takes its cards from. */
enum class DeckSource { SMART, BOOKMARKED, MISTAKES }

/** Which words a quiz asks about. */
enum class QuizSource { ALL, FAVORITES, MISTAKES, GROUP }

/** How the mode list on the quiz setup screen is drawn. */
enum class QuizViewStyle { CARD, LIST }

/** How word groups are built on the Groups screen. */
enum class GroupOrder { ALPHABETICAL, SHUFFLED }
