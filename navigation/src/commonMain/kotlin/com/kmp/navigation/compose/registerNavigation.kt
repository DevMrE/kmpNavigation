package com.kmp.navigation.compose

import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavigationGraph

/**
 * Convenience alias for [NavigationGraph.configureNavigationGraph].
 *
 * This is a **regular Kotlin function** (not composable) and can be called
 * from anywhere (e.g. application setup or before rendering the UI).
 *
 * ```kotlin
 * RegisterNavigation(
 *     startDestination = HomeScreenDestination
 * ) {
 *     section<HomeSection, MovieScreenDestination> {
 *         screen<MovieScreenDestination> { MovieScreen() }
 *         screen<SeriesScreenDestination> { SeriesScreen() }
 *     }
 * }
 * ```
 */
fun registerNavigation(
    startDestination: NavDestination,
    builder: RegisterNavigationBuilder.() -> Unit
) {
    NavigationGraph.configureNavigationGraph(startDestination, builder)
}
