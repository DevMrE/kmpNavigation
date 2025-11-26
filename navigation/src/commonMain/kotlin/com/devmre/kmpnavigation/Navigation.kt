package com.devmre.kmpnavigation

import androidx.lifecycle.ViewModel
import com.devmre.kmpnavigation.di.navigationModule

/**
 * Application-facing navigation API used by `ViewModel`s.
 *
 * Characteristics:
 * - Type-safe destinations via `D : NavDestination`.
 * - Requires a `ViewModel` context receiver for each call (see usage below).
 * - Options for singleTop, restoreState, and back stack behavior via `NavOptions`.
 *
 * Implementation:
 * - The default Compose-backed implementation is provided by `ComposeNavigation`,
 *   which is exposed as `Navigation` through DI (see [navigationModule]`).
 *   Make sure to add the [navigationModule] into your koinModules [module]
 *
 * Basic usage from a ViewModel:
 * ```kotlin
 * class MyViewModel(private val navigation: Navigation) : ViewModel() {
 *     fun openDetails(id: String) {
 *         navigation.navigateTo(MyDetailDestination(id))
 *     }
 * }
 * ```
 */
interface Navigation {

    /**
     * Navigate to the given destination. Configure behavior via [options].
     * @param navDestination The [NavDestination] to navigate
     */
    context(viewModel: ViewModel)
    fun <D : NavDestination> navigateTo(navDestination: D, options: NavOptions.() -> Unit = {})

    /**
     * Switch to a different tab destination.
     * Implemented to be singleTop + restoreState.
     */
    context(viewModel: ViewModel)
    fun <D : NavDestination> switchTab(navDestination: D)

    /**
     * Navigate up in the navigation stack.
     */
    context(viewModel: ViewModel)
    fun navigateUp()

    /**
     * Pop the back stack to [navDestination] (inclusive or not).
     * If [navDestination] is null, pops one.
     */
    context(viewModel: ViewModel)
    fun <D : NavDestination> popBackTo(
        navDestination: D? = null,
        inclusive: Boolean = false
    )
}