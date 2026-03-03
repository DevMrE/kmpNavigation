package com.kmp.navigation

/**
 * Global singleton holder for the [Navigation] instance.
 *
 * Used internally by Compose helpers and NavigationGraph.
 * In production, prefer injecting [Navigation] via Koin.
 */
data object GlobalNavigation {
    val navigation: Navigation = NavigationController()

    val controller: NavigationController
        get() = navigation as NavigationController
}