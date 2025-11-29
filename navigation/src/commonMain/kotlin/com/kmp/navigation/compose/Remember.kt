package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kmp.navigation.LocalNavigator
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Navigation
import com.kmp.navigation.NavigationFactory
import kotlinx.coroutines.flow.collectLatest

/**
 * Picks up the singleton [MutableComposeNavigation] created via [NavigationFactory.create]
 * and wires it to the provided [NavHostController].
 *
 * Also mirrors [NavHostController.currentBackStackEntryFlow] into
 * [HandleComposeNavigation.onBackstackDestinationChanged] so that:
 *  - OS back gestures
 *  - system back button
 *  - deep links
 * keep our typed destination state in sync.
 */
@Composable
internal fun rememberMutableComposeNavigation(
    navController: NavHostController,
    startDestination: NavDestination
): MutableComposeNavigation {
    // Always use the single global instance from NavigationFactory.
    val navigation = remember { NavigationFactory.mutableInstance }

    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose { navigation.detach() }
    }

    // Register the initial root tab once the graph is available.
    LaunchedEffect(navController, startDestination) {
        val rootStartId = navController.graph.startDestinationId
        HandleComposeNavigation.registerRootGraph(rootStartId, startDestination)
    }

    // Keep root-destination state in sync with the NavController back stack.
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            HandleComposeNavigation.onBackstackDestinationChanged(entry.destination.id)
        }
    }

    return navigation
}

/**
 * Returns the current [NavDestination] as tracked by the navigation layer.
 *
 * - Safe to call from any composable.
 * - Triggers recompositions whenever the active destination changes via:
 *   - navigateTo
 *   - switchTab
 *   - popBackTo
 *   - navigateUp
 *   - OS back / back gestures (mirrored through currentBackStackEntryFlow).
 *
 * [initialDestination] is used only as a fallback when no destination has been
 * registered yet (e.g. right at app start).
 */
@Composable
fun rememberNavDestination(initialDestination: NavDestination): NavDestination {
    val current by HandleComposeNavigation.currentDestinationFlow.collectAsState(
        initial = HandleComposeNavigation.currentDestinationSnapshot ?: initialDestination
    )
    return current ?: initialDestination
}

/**
 * Convenience wrapper when you only care that *some* Navigation instance is available
 * in the local composition.
 */
@Composable
fun rememberNavigation(): Navigation {
    // LocalNavigator is provided in RegisterNavigation; we keep this helper
    // so feature modules never have to know about the internal MutableComposeNavigation.
    return LocalNavigator.current
}
