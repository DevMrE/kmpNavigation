package com.kmp.navigation

/**
 * Global singleton holder for the [Navigation] instance.
 */
object GlobalNavigation {

    val navigation: Navigation by lazy { NavigationController() }

    val controller: NavigationController
        get() = navigation as NavigationController
}