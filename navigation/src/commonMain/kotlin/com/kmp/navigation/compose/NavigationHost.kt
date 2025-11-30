package com.kmp.navigation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kmp.navigation.LocalDestinationRegistry
import com.kmp.navigation.Navigation
import com.kmp.navigation.rememberNavigation

/**
 * Hosts the current destination inside your navigation tree.
 *
 * It reads the current [com.kmp.navigation.NavDestination] from [NavigationController.state] and
 * delegates rendering to the registered screen Composables in [com.kmp.navigation.DestinationRegistry].
 *
 * You typically use this inside the content of [RegisterNavigation]:
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     RegisterNavigation(
 *         startDestination = HomeScreenDestination,
 *         builder = {
 *             section<HomeSection, HomeScreenDestination> {
 *                 screen<HomeScreenDestination> { HomeScreen() }
 *             }
 *         }
 *     ) {
 *         Scaffold(
 *             topBar = { TopAppBarComponent() },
 *             bottomBar = { BottomBarComponent() }
 *         ) { padding ->
 *             NavigationHost(modifier = Modifier.padding(padding))
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun NavigationHost(
    modifier: Modifier = Modifier,
    navigation: Navigation = rememberNavigation()
) {
    val controller = navigation as? NavigationController
        ?: error("NavigationHost works only with NavigationFactory.create() implementation.")
    val registry = LocalDestinationRegistry.current

    val state by controller.state.collectAsState()
    val current = state.currentDestination ?: return

    Box(modifier = modifier) {
        registry.Render(current)
    }
}
