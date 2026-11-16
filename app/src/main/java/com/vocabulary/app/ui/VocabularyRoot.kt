package com.vocabulary.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vocabulary.app.ui.navigation.BottomNavItem
import com.vocabulary.app.ui.navigation.Screen
import com.vocabulary.app.ui.navigation.VocabularyNavHost
import com.vocabulary.app.ui.navigation.bottomNavItems

/** Screens at least this wide (tablets, foldables, landscape) get a side rail instead of a bottom bar. */
private const val WIDE_SCREEN_DP = 600

/** Content never grows wider than this, so cards and lists stay readable on large screens. */
private val MaxContentWidth = 840.dp

@Composable
fun VocabularyRoot(
    openWordId: Long? = null,
    onWordOpened: () -> Unit = {}
) {
    val navController = rememberNavController()

    // Opens the word detail when the daily word notification is tapped.
    LaunchedEffect(openWordId) {
        if (openWordId != null) {
            navController.navigate(Screen.WordDetail.createRoute(openWordId))
            onWordOpened()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun isSelected(item: BottomNavItem): Boolean =
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

    fun open(item: BottomNavItem) {
        navController.navigate(item.screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // The navigation bar or rail is visible only on top-level destinations.
    val showNavigation = bottomNavItems.any { isSelected(it) }
    val wideScreen = LocalConfiguration.current.screenWidthDp >= WIDE_SCREEN_DP

    Scaffold(
        bottomBar = {
            if (showNavigation && !wideScreen) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = isSelected(item)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { open(item) },
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = { Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            if (showNavigation && wideScreen) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    bottomNavItems.forEach { item ->
                        val selected = isSelected(item)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { open(item) },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(item.label, maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                VocabularyNavHost(
                    navController = navController,
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxSize()
                )
            }
        }
    }
}
