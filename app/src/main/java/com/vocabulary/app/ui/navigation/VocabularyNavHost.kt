package com.vocabulary.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vocabulary.app.data.model.DeckSource
import com.vocabulary.app.ui.detail.WordDetailScreen
import com.vocabulary.app.ui.home.HomeScreen
import com.vocabulary.app.ui.flashcard.FlashcardsScreen
import com.vocabulary.app.ui.flashcard.WordCardsScreen
import com.vocabulary.app.ui.groups.GroupDetailScreen
import com.vocabulary.app.ui.groups.GroupsScreen
import com.vocabulary.app.ui.list.WordsHubScreen
import com.vocabulary.app.ui.list.WordsScreen
import com.vocabulary.app.ui.quiz.QuizScreen
import com.vocabulary.app.ui.saved.SavedScreen
import com.vocabulary.app.ui.settings.SettingsScreen
import com.vocabulary.app.ui.stats.StatsScreen

@Composable
fun VocabularyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220), initialOffsetX = { it / 12 }) },
        exitTransition = { fadeOut(animationSpec = tween(160)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(160)) + slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it / 12 }) }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onWordClick = { wordId ->
                    navController.navigate(Screen.WordDetail.createRoute(wordId))
                },
                onStartFlashcards = { source ->
                    navController.navigate(Screen.Flashcards.createRoute(source))
                },
                onOpenQuiz = { navController.navigateToTab(Screen.Quiz) },
                onOpenStats = { navController.navigate(Screen.Stats.route) }
            )
        }
        composable(Screen.Words.route) {
            WordsHubScreen(
                onOpenList = { navController.navigate(Screen.WordList.route) },
                onOpenFlashcards = { navController.navigate(Screen.WordCards.route) },
                onOpenGroups = { navController.navigate(Screen.Groups.route) }
            )
        }
        composable(Screen.WordList.route) {
            WordsScreen(
                onBack = { navController.popBackStack() },
                onWordClick = { wordId ->
                    navController.navigate(Screen.WordDetail.createRoute(wordId))
                }
            )
        }
        composable(Screen.WordCards.route) {
            WordCardsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Quiz.route) {
            QuizScreen()
        }
        composable(Screen.Saved.route) {
            SavedScreen(
                onPractice = { source ->
                    navController.navigate(Screen.Flashcards.createRoute(source))
                },
                onOpenQuiz = { navController.navigateToTab(Screen.Quiz) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(
            route = Screen.Flashcards.route,
            arguments = listOf(
                navArgument(Screen.Flashcards.ARG_SOURCE) {
                    type = NavType.StringType
                    defaultValue = DeckSource.SMART.name
                }
            )
        ) {
            FlashcardsScreen(
                onBack = { navController.popBackStack() },
                onWordClick = { wordId ->
                    navController.navigate(Screen.WordDetail.createRoute(wordId))
                }
            )
        }
        composable(Screen.Groups.route) {
            GroupsScreen(
                onBack = { navController.popBackStack() },
                onOpenGroup = { index ->
                    navController.navigate(Screen.GroupDetail.createRoute(index))
                },
                onOpenQuiz = { navController.navigateToTab(Screen.Quiz) }
            )
        }
        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(
                navArgument(Screen.GroupDetail.ARG_INDEX) { type = NavType.IntType }
            )
        ) {
            GroupDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenQuiz = { navController.navigateToTab(Screen.Quiz) }
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.WordDetail.route,
            arguments = listOf(
                navArgument(Screen.WordDetail.ARG_WORD_ID) { type = NavType.LongType }
            )
        ) {
            WordDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Switches to a bottom-bar tab the same way a tap on the bar does. */
private fun NavHostController.navigateToTab(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
