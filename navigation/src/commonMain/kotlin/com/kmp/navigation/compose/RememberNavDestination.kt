package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kmp.navigation.NavDestination

/**
 * Returns the current [NavDestination] as tracked by the navigation layer.
 *
 * - Safe to call from any composable (it uses Compose state under the hood).
 * - Triggers recomposition whenever the destination changes via the
 *   navigation API (navigateTo, switchTab, popBackTo, navigateUp).
 *
 * Specify an [initialDestination], as it is possible that no navDestination has been set yet.
 */
@Composable
fun rememberNavDestination(initialDestination: NavDestination): NavDestination {
    val current by HandleComposeNavigation.currentDestinationFlow.collectAsState(
        initial = HandleComposeNavigation.currentDestinationSnapshot
    )
    return current ?: initialDestination
}
