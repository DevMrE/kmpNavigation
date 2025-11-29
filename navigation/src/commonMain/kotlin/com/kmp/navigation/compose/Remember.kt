package com.kmp.navigation.compose

import NavigationFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.kmp.navigation.LocalNavigator
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Navigation
import kotlin.reflect.KClass

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
        NavigationFactory.mutableInstance ?: NavigationImpl()
    }

    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose { navigation.detach() }
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
fun rememberNavDestination(
    initialDestination: NavDestination
): NavDestination {
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
