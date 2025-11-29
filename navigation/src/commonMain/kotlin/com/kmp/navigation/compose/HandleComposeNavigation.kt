package com.kmp.navigation.compose

import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.compose.HandleComposeNavigation.currentDestinationFlow
import com.kmp.navigation.compose.HandleComposeNavigation.onDestinationComposed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Central navigation runtime used by the KMP navigation library.
 *
 * Responsibilities:
 * - Holds the current [NavHostController].
 * - Tracks the currently shown typed [NavDestination].
 * - Implements the behavior for `navigateTo`, `switchTab`, `popBackTo`, `navigateUp`.
 *
 * The current destination is updated from within each typed screen via
 * [onDestinationComposed], so system back gestures are handled automatically.
 */
object HandleComposeNavigation {

    private const val TAG = "Navigation"

    var navController: NavHostController? = null
        private set

    private val _currentDestination = MutableStateFlow<NavDestination?>(null)

    /**
     * Flow of the currently visible typed [NavDestination].
     */
    val currentDestinationFlow: StateFlow<NavDestination?> = _currentDestination

    /**
     * Snapshot accessor used by `rememberNavDestination`.
     */
    val currentDestinationSnapshot: NavDestination?
        get() = _currentDestination.value

    /**
     * Called from each typed screen when its composable content enters composition.
     *
     * This keeps [currentDestinationFlow] in sync with whatever the `NavHost` is
     * currently displaying (including system back, gesture navigation, etc.).
     */
    fun onDestinationComposed(destination: NavDestination) {
        _currentDestination.value = destination
    }

    /**
     * Typed `navigateTo` implementation.
     *
     * - Uses the provided [NavOptions] to configure singleTop, restoreState
     *   and backstack behavior.
     * - Ignores navigation when the requested destination is already current.
     */
    fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val controller = navController ?: return

        // Avoid re-navigating to the same destination instance.
        if (currentDestinationSnapshot == navDestination) return

        val opts = NavOptions().apply(options)

        try {
            controller.navigate(navDestination) {
                launchSingleTop = opts.singleTop
                if (opts.restoreState) {
                    restoreState = true
                }

                when (val backstack = opts.backstack) {
                    is NavOptions.Backstack.PopTo -> {
                        popUpTo(backstack.navDestination) {
                            inclusive = backstack.inclusive
                            saveState = backstack.saveState
                        }
                    }

                    is NavOptions.Backstack.Clear -> {
                        // Clear everything up to the graph root.
                        popUpTo(controller.graph.id) {
                            inclusive = false
                        }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }
        } catch (e: IllegalArgumentException) {
            Logger.e(
                tag = TAG,
                messageString = e.message ?: "navigateTo($navDestination) failed",
                throwable = e
            )
        }
    }

    /**
     * Switch between high-level sections such as Home / Settings.
     *
     * The actual back stack is still managed by Navigation-Compose, so system
     * back from one section returns to whatever was below it on the stack.
     */
    fun <D : NavDestination> handleSwitchTab(navDestination: D) {
        val controller = navController ?: return

        try {
            controller.navigate(navDestination) {
                // Avoid_duplicates at the top of the stack.
                launchSingleTop = true
            }
        } catch (e: IllegalArgumentException) {
            Logger.e(
                tag = TAG,
                messageString = e.message ?: "switchTab($navDestination) failed",
                throwable = e
            )
        }
    }

    /**
     * Typed `popBackTo` implementation.
     *
     * When [navDestination] is null, it behaves like a simple `popBackStack()`.
     */
    fun <D : NavDestination> handlePopBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        val controller = navController ?: return

        if (navDestination == null) {
            controller.popBackStack()
            return
        }

        val popped = controller.popBackStack(navDestination, inclusive = inclusive)
        if (!popped) {
            controller.popBackStack()
        }
    }

    fun navigateUp() {
        navController?.navigateUp()
    }

    fun attach(controller: NavHostController) {
        navController = controller
    }

    fun detach() {
        navController = null
        _currentDestination.value = null
    }
}
