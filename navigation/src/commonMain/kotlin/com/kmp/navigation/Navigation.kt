package com.kmp.navigation

import androidx.lifecycle.ViewModel
import com.kmp.navigation.di.navigationModule
import org.koin.dsl.module

/**
 * This interface can be injected into viewModels after the navigationModule
 * has been added to koin and used for navigation within the viewModel.
 *
 * Characteristics:
 * - Type-safe destinations via [NavDestination].
 * - Options for singleTop, restoreState, and back stack behavior via [NavOptions].
 * - Make sure to add the [navigationModule] into your koinModules [module]
 *
 * Possible calling fun:
 * - [navigateTo]
 * - [switchTab] for switching between screen without adding the destination to the stack.
 * - []
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