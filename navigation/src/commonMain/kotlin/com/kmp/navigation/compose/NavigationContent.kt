package com.kmp.navigation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavigationGraph
import com.kmp.navigation.NavDestination

/**
 * Renders the current navigation destination using the registered screens.
 *
 * It reads the current [NavDestination] from [GlobalNavigation]
 * and looks up the matching Composable in [NavigationGraph].
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     Scaffold(
 *         topBar = { TopAppBarComponent() },
 *         bottomBar = { BottomBarComponent() }
 *     ) { padding ->
 *         NavigationContent(modifier = Modifier.padding(padding))
 *     }
 * }
 * ```
 */
@Composable
fun NavigationContent(
    modifier: Modifier = Modifier
) {
    val state by GlobalNavigation.controller.state.collectAsState()
    val current = state.currentDestination ?: return

    val screen = NavigationGraph.findScreen(current)
        ?: error("No screen registered for destination ${current::class.simpleName}. Did you call RegisterNavigation()?")

    Box(modifier = modifier) {
        screen(current)
    }
}
