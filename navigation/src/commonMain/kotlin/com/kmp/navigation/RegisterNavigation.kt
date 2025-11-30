package com.kmp.navigation

import com.kmp.navigation.compose.RegisterNavigationBuilder

/**
 * Configure the navigation graph with an explicit [startDestination].
 *
 * This is a regular Kotlin function and can be called from anywhere
 * (e.g. Application.onCreate, before you render your Compose UI).
 *
 * ```kotlin
 * fun configureNavigation() {
 *     RegisterNavigation(
 *         startDestination = MovieScreenDestination
 *     ) {
 *         section<HomeSection, MovieScreenDestination> {
 *             screen<MovieScreenDestination> { MovieScreen() }
 *             screen<SeriesScreenDestination> { SeriesScreen() }
 *         }
 *
 *         section<AuthSection, LoginDestination> {
 *             screen<LoginDestination> { LoginScreen() }
 *             screen<RegisterDestination> { RegisterScreen() }
 *         }
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
