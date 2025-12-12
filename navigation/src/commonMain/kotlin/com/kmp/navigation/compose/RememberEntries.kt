package com.kmp.navigation.compose

import androidx.navigation3.runtime.entryProvider
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavigationGraph
import androidx.navigation3.runtime.NavEntry

/**
 * Eigener Helper, damit der Beispielcode mit `rememberEntries(...)`
 * kompiliert. Intern nutzt er die offizielle Navigation 3 API
 * `rememberDecoratedNavEntries(...)`.
 */
fun createEntryProviderWithDsl(
    graph: NavigationGraph
): (NavDestination) -> NavEntry<NavDestination> =
    entryProvider {
        // Ein catch-all Entry: alle NavDestination-Instanzen laufen hier durch
        entry<NavDestination> { destination ->
            val screen = NavigationGraph.findScreen(destination)
                ?: error(
                    "No screen registered for destination " +
                            "${destination::class.simpleName}. Did you call registerNavigation()?"
                )

            screen(destination)
        }
    }