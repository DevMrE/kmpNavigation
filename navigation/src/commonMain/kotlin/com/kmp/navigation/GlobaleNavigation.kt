package com.kmp.navigation

/**
 * Global singleton holder for the [Navigation] instance.
 *
 * Used internally by Compose helpers and NavigationGraph.
 * In production, prefer injecting [Navigation] via Koin.
 */
object GlobalNavigation {

    val navigation: Navigation = NavigationFactory.create()

    val controller: NavigationController
        get() = navigation as NavigationController
}