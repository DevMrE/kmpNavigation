package com.kmp.navigation

/**
 * Entry point for configuring the navigation graph.
 *
 * Call this once during app startup before rendering any Compose UI.
 *
 * ```kotlin
 * fun setupNavigation() {
 *     registerNavigation(startDestination = MovieDestination) {
 *         section(AppRootSection, AppRootDestination) {
 *             screen<AppRootDestination> { AppRootScreen() }
 *
 *             section(HomeSection, HomeDestination) {
 *                 screen<HomeDestination> { HomeScreen() }
 *                 screen<MovieDestination> { MovieScreen() }
 *                 screen<SeriesDestination> { SeriesScreen() }
 *             }
 *
 *             screen<SettingsDestination> { SettingsScreen() }
 *         }
 *
 *         section(DetailSection, DetailDestination(id = "")) {
 *             screen<DetailDestination> { dest -> DetailScreen(dest.id) }
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