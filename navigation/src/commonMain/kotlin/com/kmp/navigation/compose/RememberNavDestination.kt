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
 * The result can be `null`:
 * - before the first navigation call, or
 * - right after a back operation where the library cannot resolve
 *   a concrete destination (for example a generic `popBackTo(null, inclusive = true)`).
 */
@Composable
fun rememberNavDestination(initialDestination: NavDestination): NavDestination {
    val current by HandleComposeNavigation.currentDestinationFlow.collectAsState(
        initial = HandleComposeNavigation.currentDestinationSnapshot
    )
    return current ?: initialDestination
}
