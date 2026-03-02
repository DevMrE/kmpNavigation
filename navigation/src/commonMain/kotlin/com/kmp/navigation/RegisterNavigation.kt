package com.kmp.navigation

/**
 * Convenience alias for [NavigationGraph.configureNavigationGraph].
 *
 * Call this once during app setup before rendering any Compose UI:
 *
 * ```kotlin
 * fun registerAppNavigation() {
 *     registerNavigation(startDestination = AppRootDestination) {
 *
 *         section<AppRootSection>(root = AppRootDestination) {
 *
 *             section<HomeSection>(root = MovieScreenDestination) {
 *                 screen<MovieScreenDestination> { MovieScreen() }
 *                 screen<SeriesScreenDestination> { SeriesScreen() }
 *             }
 *
 *             section<SettingsSection>(root = SettingsScreenDestination) {
 *                 screen<SettingsScreenDestination> { SettingsScreen() }
 *             }
 *         }
 *
 *         section<DetailSection>(root = DetailScreenDestination(id = ""), overlay = true) {
 *             screen<DetailScreenDestination> { detail -> DetailScreen(detail.id) }
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