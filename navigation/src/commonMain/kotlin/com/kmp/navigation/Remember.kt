package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kmp.navigation.compose.NavigationImpl
import kotlin.reflect.KClass

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
fun rememberNavDestination(): NavDestination? {
    val navigation = rememberNavigation()
    val controller = navigation as? NavigationImpl
        ?: error("rememberNavDestination() funktioniert nur mit der NavigationFactory.create()-Implementation.")
    val state by controller.state.collectAsState()
    return state.currentDestination
}