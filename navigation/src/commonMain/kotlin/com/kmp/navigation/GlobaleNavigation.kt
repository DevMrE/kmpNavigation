package com.kmp.navigation

import com.kmp.navigation.compose.NavigationController

/**
 * Global navigation holder used by the compose helpers and the default DI module.
 *
 * There is exactly one [Navigation] instance for your app by default.
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
     * The single [Navigation] instance used by this library.
     *
     * ```kotlin
     * val navigation: Navigation = GlobalNavigation.navigation
     * navigation.navigateTo(HomeScreenDestination)
     * ```
     */
    val navigation: Navigation = NavigationFactory.create()

    /**
     * Typed access to the underlying [NavigationController].
     * Used internally by compose helpers to observe navigation state.
     */
    internal val controller: NavigationController
        get() = navigation as NavigationController
}
