package com.kmp.navigation

import com.kmp.navigation.compose.NavigationController

/**
 * Factory that creates the default [Navigation] implementation.
 *
 * Use this when you do not rely on a DI container, or inside your DI
 * configuration to provide a singleton navigation instance.
 *
 * ```kotlin
 * // Without DI – create a navigation instance manually
 * val navigation: Navigation = NavigationFactory.create()
 *
 * // With Koin – provide a singleton
 * val navigationModule = module {
 *     single<Navigation> { NavigationFactory.create() }
 * }
 * ```
 */
object NavigationFactory {

    /**
     * Create a new [Navigation] instance backed by [NavigationController].
     *
     * ```kotlin
     * val navigation: Navigation = NavigationFactory.create()
     * navigation.navigateTo(HomeScreenDestination)
     * ```
     */
    fun create(): Navigation = NavigationController()
}
