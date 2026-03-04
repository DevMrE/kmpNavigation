package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.kmp.navigation.*

/**
 * Returns the singleton [Navigation] instance.
 *
 * ```kotlin
 * val navigation = rememberNavigation()
 * navigation.navigateTo(DetailScreenDestination("42"))
 * ```
 */
@Composable
fun rememberNavigation(): Navigation = remember { NavigationFactory.create() }

/**
 * Observes the current destination at the top of the BackStack.
 *
 * ```kotlin
 * val current = rememberNavDestination()
 * ```
 */
@Composable
fun rememberNavDestination(): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentDestination
}

/**
 * Observes the currently active destination within a tabs group [Tab].
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
inline fun <reified Tab : NavTabs> rememberActiveTabIn(): NavDestination? {
    val controller = NavigationFactory.controller()
    val state by controller.state.collectAsState()
    // Re-compute whenever backStack or lastEvent changes
    return remember(state.backStack, state.lastEvent) {
        controller.activeDestinationFor(Tab::class)
    }
}

/**
 * Returns true if the current top of the BackStack belongs to the tabs group [Tab]
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
inline fun <reified Tab : NavTabs> rememberIsTabsActive(): Boolean {
    val controller = NavigationFactory.controller()
    val state by controller.state.collectAsState()

    return remember(state.backStack) {
        val destinations = NavigationGraph.destinationsFor(Tab::class)
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
        val groupClass = NavigationGraph.findTabs(destination)
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