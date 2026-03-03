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
 */
@Composable
fun rememberNavigation(): Navigation =
    remember { GlobalNavigation.navigation }

/**
 * Observes the current global [NavDestination].
 *
 * Note: This returns the LAST destination in the entire back stack.
 * For tab highlighting within a section, use
 * [rememberCurrentDestinationInSection] instead.
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
 * Use this for tab highlighting – it returns the last destination
 * in the back stack that belongs to section [S] or any sub-section of [S].
 *
 * This is different from [rememberNavDestination] which returns the global
 * last destination regardless of section.
 *
 * ```kotlin
 * // Correct way to highlight tabs in HomeScreen:
 * val currentTab = rememberCurrentDestinationInSection<HomeScreenSection>()
 *
 * SegmentedButton(
 *     selected = currentTab is MovieScreenDestination,
 *     onClick = { navigation.switchTo(MovieScreenDestination) }
 * )
 * SegmentedButton(
 *     selected = currentTab is SeriesScreenDestination,
 *     onClick = { navigation.switchTo(SeriesScreenDestination) }
 * )
 * ```
 */
@Composable
inline fun <reified S : NavSection> rememberCurrentDestinationInSection(): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.backStack.lastOrNull {
        NavigationGraph.destinationBelongsToSectionScope(it, S::class)
    }
}