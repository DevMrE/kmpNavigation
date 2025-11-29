package com.kmp.navigation.compose

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import com.kmp.navigation.DefaultRouteIdProvider
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.RouteIdProvider
import com.kmp.navigation.TypedDestinationRegistry
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Internal bridge between:
 *
 * - the public [Navigation] API used from ViewModels / Composables
 * - the underlying [NavHostController] from Navigation-Compose
 *
 * Responsibilities:
 * - Attach/detach the NavHostController
 * - Execute typed navigation calls (navigateTo, switchTab, popBackTo, navigateUp)
 * - Track the *current* typed destination KClass via [currentDestinationClassFlow]
 */
internal object HandleComposeNavigation {

    var navController: NavHostController? = null
        private set

    private val routeIdProvider: RouteIdProvider = DefaultRouteIdProvider

    // --- state exposed to the Compose layer -----------------------------------

    private val _currentDestinationClass =
        MutableStateFlow<KClass<out NavDestination>?>(null)

    val currentDestinationClassFlow: StateFlow<KClass<out NavDestination>?> =
        _currentDestinationClass

    val currentDestinationClassSnapshot: KClass<out NavDestination>?
        get() = _currentDestinationClass.value

    private val _currentRootClass =
        MutableStateFlow<KClass<out NavDestination>?>(null)

    // --- lifecycle -------------------------------------------------------------

    fun attach(controller: NavHostController) {
        navController = controller
    }

    fun detach() {
        navController = null
        _currentDestinationClass.value = null
        _currentRootClass.value = null
    }

    /**
     * Called from the Compose layer whenever Navigation-Compose reports that the
     * current backstack entry has changed (including system back / gestures).
     *
     * We translate the entry's route into a typed [NavDestination] KClass using
     * [TypedDestinationRegistry].
     */
    fun onBackstackEntryChanged(entry: NavBackStackEntry) {
        val route = entry.destination.route ?: return
        val destClass = TypedDestinationRegistry.classForRoute(route) ?: run {
            Logger.d("Navigation") {
                "Unknown typed destination route=$route; not registered " +
                        "via TypedGraphBuilder.screen/section."
            }
            return
        }

        _currentDestinationClass.value = destClass
        _currentRootClass.value = TypedDestinationRegistry.rootForClass(destClass)
    }

    // --- public navigation operations used by NavigationImpl -------------------

    fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val controller = navController ?: return

        val targetId = idOf(navDestination)
        val currentId = controller.currentDestination?.id

        // Avoid re-navigating to the same destination ID
        if (currentId == targetId) return

        try {
            val opts = NavOptions().apply(options)

            controller.navigate(navDestination) {
                launchSingleTop = opts.singleTop
                if (opts.restoreState) restoreState = true

                when (val backstack = opts.backstack) {
                    is NavOptions.Backstack.PopTo -> {
                        popUpTo(backstack.navDestination) {
                            inclusive = backstack.inclusive
                            saveState = backstack.saveState
                        }
                    }

                    is NavOptions.Backstack.Clear -> {
                        // Clear the entire back stack (up to the graph root)
                        controller.graph.id.let { graphId ->
                            popUpTo(graphId) { inclusive = false }
                        }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }
        } catch (e: IllegalArgumentException) {
            Logger.e("Navigation") {
                "navigateTo(${navDestination::class.simpleName}) failed: ${e.message}"
            }
        }
    }

    /**
     * Switch between "root" destinations (tabs / sections).
     *
     * The root concept is defined by [TypedDestinationRegistry]:
     * - A screen declared via `screen<Dest>` outside any `section` is its own root.
     * - A screen declared inside `section<Parent, ...>` has [Parent] as root.
     *
     * This method:
     * - does nothing if the target root is already active
     * - otherwise navigates to [navDestination] and clears the previous root from
     *   the backstack so system back does not jump to the previous tab.
     */
    fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        val controller = navController ?: return

        val targetClass = navDestination::class
        val targetRoot = TypedDestinationRegistry.rootForClass(targetClass)
        val currentRoot = _currentRootClass.value

        // Already on this root → no-op
        if (currentRoot == targetRoot) return

        try {
            controller.navigate(navDestination) {
                launchSingleTop = true

                // Treat root switches as "replace root":
                // clear backstack up to the start destination of the graph.
                popUpTo(controller.graph.startDestinationId) {
                    inclusive = true
                }
            }
        } catch (e: Exception) {
            Logger.e("Navigation") {
                "switchTab(${navDestination::class.simpleName}) failed: ${e.message}"
            }
        }
    }

    fun <D : NavDestination> handlePopBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        val controller = navController ?: return

        if (navDestination == null) {
            controller.popBackStack()
        } else {
            val ok = controller.popBackStack(navDestination, inclusive)
            if (!ok) controller.popBackStack()
        }
    }

    fun navigateUp() {
        navController?.navigateUp()
    }

    // --- helper ----------------------------------------------------------------

    private fun <D : NavDestination> idOf(destination: D): Int =
        routeIdProvider.idFor(destination::class)
}
