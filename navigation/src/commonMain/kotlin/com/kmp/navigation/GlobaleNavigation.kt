package com.kmp.navigation

import com.kmp.navigation.compose.rememberNavigation

/**
 * Global holder for the single [Navigation] instance used by the app.
 *
 * This is the instance that [rememberNavigation] uses and that you can
 * also expose via DI:
 *
 * ```kotlin
 * // Koin module
 * val navigationModule = module {
 *     single<Navigation> { GlobalNavigation.navigation }
 * }
 * ```
 */
object GlobalNavigation {

    /**
     * The global [Navigation] instance.
     *
     * ```kotlin
     * val navigation: Navigation = GlobalNavigation.navigation
     * ```
     */
    val navigation: Navigation = NavigationFactory.create()

    /**
     * Typed access to the underlying [NavigationController].
     * Used internally by compose helpers and the navigation graph.
     */
    internal val controller: NavigationController
        get() = navigation as NavigationController
}
