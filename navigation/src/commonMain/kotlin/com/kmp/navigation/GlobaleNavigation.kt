package com.kmp.navigation

/**
 * Global singleton holder for the [Navigation] instance.
 */
@PublishedApi
internal object GlobalNavigation {
    val navigation: Navigation by lazy { NavigationController() }

    @PublishedApi
    internal val controller: NavigationController
        get() = navigation as NavigationController
}