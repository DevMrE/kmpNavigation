package com.kmp.navigation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph

/**
 * Renders the current navigation destination using the registered screens
 * and applies animations based on the last navigation event:
 *
 * * [NavigationEvent.NavigateTo]  -> fade in / fade out
 * * [NavigationEvent.SwitchTo]   -> horizontal slide based on section index
 * * [NavigationEvent.NavigateUp] -> fade
 * * [NavigationEvent.PopBackTo]  -> fade
 *
 * Section indices are derived from the declaration order of `section<...>()`
 * in your `registerNavigation` DSL.
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     // navigation graph has been configured via registerNavigation(...)
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
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
) {
    // If no graph has been registered, show fallback
    if (!NavigationGraph.isConfigured()) {
        fallbackContent()
        return
    }

    val navigation = rememberNavigation()
    val state by GlobalNavigation.controller.state.collectAsState()

    // Mirror the controller back stack into a Nav3-style SnapshotStateList
    // This is what NavDisplay observes and animates.
    val backStack = remember { mutableStateListOf<NavDestination>() }

    LaunchedEffect(state.backStack) {
        backStack.clear()
        backStack.addAll(state.backStack)
    }

    // If there is still nothing to show, display fallback content
    if (backStack.isEmpty()) {
        fallbackContent()
        return
    }

    // NavDisplay is the Navigation 3 UI component which handles transitions, gestures etc.
    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = {
            // Delegate back events to your NavigationController
            // This will update GlobalNavigation.controller.state,
            // which in turn updates [backStack] via the LaunchedEffect above.
            navigation.navigateUp()
        },
        entryProvider = entryProvider {
            // Single generic entry for all NavDestination implementations.
            // The actual content mapping is still defined entirely
            // by your registerNavigation() DSL via NavigationGraph.
            entry<NavDestination> { destination ->
                val screen = NavigationGraph.findScreen(destination)
                    ?: error(
                        "No screen registered for destination " +
                                "${destination::class.simpleName}. Did you call registerNavigation()?"
                    )

                Box(modifier = Modifier.fillMaxSize()) {
                    screen(destination)
                }
            }
        }
    )
}
