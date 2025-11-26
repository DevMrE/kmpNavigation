package com.devmre.kmpnavigation.compose_interface

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.devmre.kmpnavigation.NavOptions
import com.devmre.kmpnavigation.NavDestination

/**
 * Compose-backed implementation of the `Navigation` API.
 *
 * Responsibilities:
 * - Holds a reference to a `NavHostController` (attached/detached via `MutableComposeNavigation`).
 * - Implements typed navigation calls (`navigateTo`, `switchTab`, `navigateUp`, `popBackTo`).
 * - Automatically remembers the last visited destination per "root tab" (section<G, S>).
 */
internal class ComposeNavigation(
    private val handleNavigation: HandleNavigation
) : MutableComposeNavigation {

    override fun attach(controller: NavHostController) {
        handleNavigation.attach(controller)
    }

    override fun detach() {
        handleNavigation.detach()
    }

    context(viewModel: ViewModel)
    override fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        handleNavigateTo(navDestination, options)
    }

    context(viewModel: ViewModel)
    override fun <D : NavDestination> switchTab(navDestination: D) {
        handleSwitchTo(navDestination)
    }

    context(viewModel: ViewModel)
    override fun navigateUp() {
        handleNavigation.navigateUp()
    }

    context(viewModel: ViewModel)
    override fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        handleNavigation.handlePopBackTo(navDestination, inclusive)
    }

    private fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        handleNavigation.handleNavigateTo(navDestination, options)
    }

    private fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        handleNavigation.handleSwitchTo(navDestination)
    }
}
