package com.kmp.navigation

/**
 * Convenience alias for [NavigationGraph.configureNavigationGraph].
 *
 * This is **not** a composable. You call it once during app setup to
 * configure the navigation graph:
 *
 * ```kotlin
 * fun registerAppNavigation() {
 *     registerNavigation(startDestination = MovieScreenDestination) {
 *         section<HomeSection, MovieScreenDestination> {
 *             screen<MovieScreenDestination> { MovieScreen() }
 *             screen<SeriesScreenDestination> { SeriesScreen() }
 *         }
 *
 *         section<SettingsSection, SettingsScreenDestination> {
 *             screen<SettingsScreenDestination> { SettingsScreen() }
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
