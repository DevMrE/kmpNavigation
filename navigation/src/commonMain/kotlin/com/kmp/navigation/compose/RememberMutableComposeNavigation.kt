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
            ?: error(
                "NavigationFactory.create() must be called " +
                        "(e.g. via DI: single<Navigation> { NavigationFactory.create() }) " +
                        "before RegisterNavigation is used."
            )
    }

    // Attach/detach the NavController to the navigation implementation
    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose { navigation.detach() }
    }

    // Mirror NavController back stack changes into HandleComposeNavigation so that
    // rememberNavDestination also reacts to OS back gestures and navigateUp/popBackStack.
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            HandleComposeNavigation.onBackstackDestinationChanged(entry.destination.id)
        }
    }

    return navigation
}
