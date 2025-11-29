package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.kmp.navigation.LocalNavigator
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Navigation
import com.kmp.navigation.NavigationFactory
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.collectLatest

/**
 * Binds the shared Navigation implementation to the given [NavHostController]:
 *
 * - gets/creates the [MutableComposeNavigation] instance via [NavigationFactory]
 * - attaches the [NavHostController] to [HandleComposeNavigation]
 * - forwards all backstack changes to [HandleComposeNavigation.onBackstackEntryChanged]
 */
@Composable
internal fun rememberMutableComposeNavigation(
    navController: NavHostController
): MutableComposeNavigation {
    val navigation = remember {
        // Always go through the factory so we share the same instance
        (NavigationFactory.mutableInstance as? MutableComposeNavigation)
            ?: (NavigationFactory.create() as MutableComposeNavigation)
    }

    // Attach / detach the NavController
    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose { navigation.detach() }
    }

    // Mirror Navigation-Compose backstack changes into our navigation layer
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            HandleComposeNavigation.onBackstackEntryChanged(entry)
        }
    }

    return navigation
}

/**
 * Returns the *type* of the current [NavDestination] as a [KClass].
 *
 * This is safe to use from any composable and will recompose whenever
 * the underlying Navigation-Compose backstack changes.
 *
 * Example:
 * ```kotlin
 * val current = rememberNavDestinationClass(HomeScreenDestination::class)
 *
 * NavigationBarItem(
 *     selected = current == HomeScreenDestination::class,
 *     onClick = { navigation.switchTab(HomeScreenDestination) }
 * )
 * ```
 */
@Composable
fun rememberNavDestinationClass(
    initialDestination: KClass<out NavDestination>
): KClass<out NavDestination> {
    val current by HandleComposeNavigation.currentDestinationClassFlow.collectAsState(
        initial = HandleComposeNavigation.currentDestinationClassSnapshot ?: initialDestination
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
