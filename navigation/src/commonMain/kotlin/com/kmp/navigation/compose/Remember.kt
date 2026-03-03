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

@Composable
fun rememberNavigation(): Navigation =
    remember { GlobalNavigation.navigation }

@Composable
fun rememberNavDestination(fallback: NavDestination? = null): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentDestination ?: fallback
}

@Composable
fun rememberNavSection(fallback: NavSection? = null): NavSection? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentSection ?: fallback
}

/**
 * Returns the parent section of the currently active section.
 *
 * Useful for showing/hiding UI elements based on the parent section.
 *
 * ```kotlin
 * // Show BottomBar only when AppRootSection or any child is active
 * val parentSection = rememberParentSection()
 * val isVisible = parentSection == AppRootSection
 *                 || rememberNavSection() == AppRootSection
 * ```
 */
@Composable
fun rememberParentSection(): NavSection? {
    val state by GlobalNavigation.controller.state.collectAsState()
    val currentSection = state.currentSection ?: return null
    return GlobalNavigation.controller.parentSectionOf(currentSection)
}

@Composable
inline fun <reified S : NavSection> rememberCurrentDestinationInSection(): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.backStack.lastOrNull {
        NavigationGraph.destinationBelongsToSectionScope(it, S::class)
    }
}