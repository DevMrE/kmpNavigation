package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.kmp.navigation.NavigationFactory
import kotlinx.coroutines.flow.collectLatest

/**
 * Bridge between Compose NavHost and the KMP navigation implementation.
 *
 * - Obtains the shared [MutableComposeNavigation] instance from [NavigationFactory].
 * - Attaches / detaches the [NavHostController] to/from [HandleComposeNavigation].
 * - Listens to [NavHostController.currentBackStackEntryFlow] and forwards
 *   destination ID changes to [HandleComposeNavigation.onBackstackDestinationChanged].
 */
@Composable
internal fun rememberMutableComposeNavigation(
    navController: NavHostController
): MutableComposeNavigation {
    val navigation = remember {
        NavigationFactory.mutableInstance
            ?: error("NavigationFactory.create() must be called before RegisterNavigation()")
    }

    // Keep navController attached to our internal handler for the lifetime
    // of this NavHost.
    DisposableEffect(navigation, navController) {
        HandleComposeNavigation.attach(navController)
        navigation.attach(navController)

        onDispose {
            navigation.detach()
            HandleComposeNavigation.detach()
        }
    }

    // Mirror NavController backstack changes into HandleComposeNavigation
    LaunchedEffect(navController) {
        // Initial destination (if available)
        navController.currentBackStackEntry?.destination?.id?.let { id ->
            HandleComposeNavigation.onBackstackDestinationChanged(id)
        }

        // All subsequent changes (system back, gestures, popBackStack, ...)
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            HandleComposeNavigation.onBackstackDestinationChanged(entry.destination.id)
        }
    }

    return navigation
}
