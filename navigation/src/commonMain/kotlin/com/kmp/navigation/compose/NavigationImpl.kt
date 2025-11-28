package com.kmp.navigation.compose

import androidx.navigation.NavHostController
import com.kmp.navigation.NavOptions
import com.kmp.navigation.NavDestination

/**
 * Compose-backed implementation of the `Navigation` API.
 *
 * Responsibilities:
 * - Holds a reference to a `NavHostController` (attached/detached via `MutableComposeNavigation`).
 * - Implements typed navigation calls (`navigateTo`, `switchTab`, `navigateUp`, `popBackTo`).
 * - Automatically remembers the last visited destination per "root tab" (section<G, S>).
 */
class NavigationImpl : MutableComposeNavigation {

    override fun attach(controller: NavHostController) {
        HandleComposeNavigation.attach(controller)
    }

    override fun detach() {
        HandleComposeNavigation.detach()
    }

    override fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        HandleComposeNavigation.handleNavigateTo(navDestination, options)
    }

    override fun <D : NavDestination> switchTab(navDestination: D) {
        HandleComposeNavigation.handleSwitchTo(navDestination)
    }

    override fun navigateUp() {
        HandleComposeNavigation.navigateUp()
    }

    override fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        HandleComposeNavigation.handlePopBackTo(navDestination, inclusive)
    }
}
