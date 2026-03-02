package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.Navigation
import com.kmp.navigation.NavigationGraph

/**
 * Returns the singleton [Navigation] instance.
 *
 * ```kotlin
 * @Composable
 * fun BottomBarComponent() {
 *     val navigation = rememberNavigation()
 *     NavigationBarItem(onClick = { navigation.switchTo(HomeSection) })
 * }
 * ```
 */
@Composable
fun rememberNavigation(): Navigation =
    remember { GlobalNavigation.navigation }

/**
 * Observes the current [NavDestination] from the global navigation state.
 *
 * ```kotlin
 * @Composable
 * fun TopAppBarComponent() {
 *     val destination = rememberNavDestination()
 * }
 * ```
 */
@Composable
fun rememberNavDestination(
    initialDestination: NavDestination? = null
): NavDestination {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentDestination
        ?: initialDestination
        ?: error(
            "No current NavDestination and no initialDestination provided. " +
                    "Make sure registerNavigation() was called before rendering."
        )
}

/**
 * Observes the current [NavSection].
 *
 * ```kotlin
 * @Composable
 * fun BottomBarComponent() {
 *     val section = rememberNavSection(initialSection = HomeSection)
 *     NavigationBarItem(selected = section == HomeSection)
 * }
 * ```
 */
@Composable
fun rememberNavSection(
    initialSection: NavSection? = null
): NavSection {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentSection
        ?: initialSection
        ?: error(
            "No current NavSection and no initialSection provided. " +
                    "Make sure registerNavigation() was called before rendering."
        )
}

/**
 * Observes the currently active destination within section [S].
 *
 * Use this to highlight the correct tab in a tab bar.
 *
 * Returns the last destination in the back stack that belongs to section [S],
 * or null if no destination in [S] is currently in the stack.
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val navigation = rememberNavigation()
 *     val currentTab = rememberCurrentDestinationInSection<HomeSection>()
 *
 *     TabRow(selectedTabIndex = when (currentTab) {
 *         is MovieScreenDestination -> 0
 *         else -> 1
 *     }) {
 *         Tab(
 *             selected = currentTab is MovieScreenDestination,
 *             onClick = { navigation.switchTo(MovieTab) }
 *         )
 *         Tab(
 *             selected = currentTab is SeriesScreenDestination,
 *             onClick = { navigation.switchTo(SeriesTab) }
 *         )
 *     }
 * }
 * ```
 */
@Composable
inline fun <reified S : NavSection> rememberCurrentDestinationInSection(): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.backStack.lastOrNull {
        NavigationGraph.destinationBelongsToSectionScope(it, S::class)
    }
}