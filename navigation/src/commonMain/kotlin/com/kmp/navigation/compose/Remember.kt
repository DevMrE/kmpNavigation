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
 * fun BottomBar() {
 *     val navigation = rememberNavigation()
 *     NavigationBarItem(onClick = { navigation.switchTo(HomeSection) })
 * }
 * ```
 */
@Composable
fun rememberNavigation(): Navigation =
    remember { GlobalNavigation.navigation }

/**
 * Observes the current [NavDestination].
 *
 * Returns [fallback] if no destination is active yet.
 *
 * ```kotlin
 * val destination = rememberNavDestination()
 * ```
 */
@Composable
fun rememberNavDestination(
    fallback: NavDestination? = null
): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentDestination ?: fallback
}

/**
 * Observes the current [NavSection].
 *
 * Returns [fallback] if no section is active yet.
 *
 * ```kotlin
 * val section = rememberNavSection(fallback = HomeSection)
 * ```
 */
@Composable
fun rememberNavSection(
    fallback: NavSection? = null
): NavSection? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentSection ?: fallback
}

/**
 * Observes the currently active destination within section [S].
 *
 * Returns the last destination in the back stack that belongs to [S],
 * or null if none found.
 *
 * ```kotlin
 * val currentTab = rememberCurrentDestinationInSection<HomeSection>()
 * TabRow(selectedTabIndex = when (currentTab) {
 *     is MovieDestination -> 0
 *     else -> 1
 * })
 * ```
 */
@Composable
inline fun <reified S : NavSection> rememberCurrentDestinationInSection(): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.backStack.lastOrNull {
        NavigationGraph.destinationBelongsToSectionScope(it, S::class)
    }
}