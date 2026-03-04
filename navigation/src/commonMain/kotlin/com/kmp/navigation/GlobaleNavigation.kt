package com.kmp.navigation

/**
 * Global singleton holder for the [Navigation] instance.
 */
internal object GlobalNavigation {
    internal val navigation: Navigation by lazy { NavigationController() }

    internal val controller: NavigationController
        get() = navigation as NavigationController
}