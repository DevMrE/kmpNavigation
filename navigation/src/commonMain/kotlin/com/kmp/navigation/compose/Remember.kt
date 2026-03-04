package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavGroup
import com.kmp.navigation.Navigation
import com.kmp.navigation.NavigationGraph

/**
 * Returns the singleton [Navigation] instance.
 *
 * ```kotlin
 * val navigation = rememberNavigation()
 * navigation.navigateTo(DetailScreenDestination("42"))
 * ```
 */
@Composable
fun rememberNavigation(): Navigation = remember { GlobalNavigation.navigation }

/**
 * Observes the current destination at the top of the BackStack.
 *
 * ```kotlin
 * val current = rememberCurrentDestination()
 * ```
 */
@Composable
fun rememberCurrentDestination(): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentDestination
}

/**
 * Observes the currently active destination within a tabs group [G].
 *
 * Use this for highlighting the active tab in a tab bar.
 *
 * ```kotlin
 * val activeTab = rememberActiveTabIn<HomeTabs>()
 *
 * SegmentedButton(
 *     selected = activeTab is MovieContentDestination,
 *     onClick = { navigation.navigateTo(MovieContentDestination) }
 * )
 * ```
 */
@Composable
inline fun <reified G : NavGroup> rememberActiveTabIn(): NavDestination? {
    val controller = GlobalNavigation.controller
    val state by controller.state.collectAsState()
    // Re-compute whenever backStack or lastEvent changes
    return remember(state.backStack, state.lastEvent) {
        controller.activeDestinationFor(G::class)
    }
}

/**
 * Returns true if the current top of the BackStack belongs to the tabs group [G]
 * or any of its destinations.
 *
 * Use this to determine if a tab group is "active" – e.g. to highlight
 * the Home icon in a BottomBar even when a screen destination is on top.
 *
 * ```kotlin
 * val isHomeActive = rememberIsGroupActive<AppRoot>()
 *
 * NavigationBarItem(
 *     selected = isHomeActive,
 *     onClick = { navigation.navigateTo(HomeContentDestination) }
 * )
 * ```
 */
@Composable
inline fun <reified G : NavGroup> rememberIsGroupActive(): Boolean {
    val controller = GlobalNavigation.controller
    val state by controller.state.collectAsState()

    return remember(state.backStack) {
        val destinations = NavigationGraph.destinationsFor(G::class)
        val destClasses = destinations.map { it::class }.toSet()

        // Group is active if any destination in the BackStack belongs to it
        state.backStack.any { it::class in destClasses }
    }
}

/**
 * Observes whether a specific destination is currently active in its group.
 *
 * ```kotlin
 * val isMovieActive = rememberIsDestinationActive(MovieContentDestination)
 *
 * SegmentedButton(selected = isMovieActive, ...)
 * ```
 */
@Composable
fun rememberIsDestinationActive(destination: NavDestination): Boolean {
    val controller = GlobalNavigation.controller
    val state by controller.state.collectAsState()

    return remember(state.backStack, state.lastEvent) {
        val groupClass = NavigationGraph.groupOf(destination)
        if (groupClass != null) {
            // Tab destination – check if it's the active one in its group
            controller.activeDestinationFor(groupClass)?.let {
                it::class == destination::class
            } ?: false
        } else {
            // Screen/content destination – check if it's on top of BackStack
            state.currentDestination?.let {
                it::class == destination::class
            } ?: false
        }
    }
}

/**
 * Returns the full current BackStack.
 *
 * ```kotlin
 * val backStack = rememberBackStack()
 * ```
 */
@Composable
fun rememberBackStack(): List<NavDestination> {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.backStack
}