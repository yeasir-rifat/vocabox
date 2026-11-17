package com.vocabulary.app.ui.navigation

import com.vocabulary.app.ui.components.AppIcons
import androidx.compose.ui.graphics.vector.ImageVector
import com.vocabulary.app.data.model.DeckSource
import com.vocabulary.app.data.model.GroupOrder
import com.vocabulary.app.data.model.WordType

/** All navigation destinations of the app. */
sealed class Screen(val route: String) {

    // Top-level destinations shown in the bottom bar.
    data object Home : Screen("home")
    data object Words : Screen("words")
    data object Quiz : Screen("quiz")
    data object Saved : Screen("saved")
    data object Settings : Screen("settings")

    // Secondary destinations.
    data object Flashcards : Screen("flashcards?source={source}") {
        const val ARG_SOURCE = "source"
        fun createRoute(source: DeckSource = DeckSource.SMART) = "flashcards?source=${source.name}"
    }
    data object WordList : Screen("words/list")
    data object WordCards : Screen("words/cards")
    data object Stats : Screen("stats")
    data object Admin : Screen("admin")
    data object Groups : Screen("groups")
    data object GroupDetail : Screen("groups/{index}") {
        const val ARG_INDEX = "index"
        fun createRoute(index: Int) = "groups/$index"
    }
    data object WordDetail : Screen("word/{wordId}") {
        const val ARG_WORD_ID = "wordId"
        fun createRoute(wordId: Long) = "word/$wordId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    /** Outline icon shown when the tab is not selected. */
    val icon: ImageVector,
    /** Filled icon shown when the tab is selected. */
    val selectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", AppIcons.Home, AppIcons.HomeFilled),
    BottomNavItem(Screen.Words, "Words", AppIcons.List, AppIcons.List),
    BottomNavItem(Screen.Quiz, "Quiz", AppIcons.Pencil, AppIcons.Pencil),
    BottomNavItem(Screen.Saved, "Saved", AppIcons.Star, AppIcons.StarFilled),
    BottomNavItem(Screen.Settings, "Settings", AppIcons.Settings, AppIcons.Settings)
)
