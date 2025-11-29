package com.kmp.navigation.compose

import androidx.navigation.NavDestination as AndroidNavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Central navigation coordinator for Compose Navigation.
 *
 * Responsibilities:
 * - Holds the current [NavHostController].
 * - Tracks the *root* destination (e.g. Home vs Settings) for UI (BottomBar, TopBar).
 * - Implements high-level behaviors for:
 *   - [handleNavigateTo]   – normal in-graph navigation.
 *   - [handleSwitchTo]     – tab/root navigation with multi-backstack pattern.
 *   - [handlePopBackTo] / [navigateUp].
 *
 * Root tracking:
 * - We do *not* try to reconstruct every typed child destination from the backstack.
 * - Instead we map each top-level graph (root node in the NavGraph tree) to a
 *   typed root [NavDestination] (HomeScreenDestination, SettingsScreenDestination, ...).
 * - That mapping is enough to:
 *     - avoid redundant tab navigations
 *     - drive [rememberNavDestination] for UI state.
 */
internal object HandleComposeNavigation {

    private const val TAG = "Navigation"

    var navController: NavHostController? = null
        private set

    /**
     * Maps root-graph node IDs (top-level children of the NavHost graph)
     * to their typed root destination (e.g. HomeScreenDestination, SettingsScreenDestination).
     */
    private val rootDestinationByRootId = mutableMapOf<Int, NavDestination>()

    /**
     * Current root destination (for example "Home" or "Settings").
     */
    private val _currentRootDestination = MutableStateFlow<NavDestination?>(null)
    val currentDestinationFlow: StateFlow<NavDestination?> = _currentRootDestination

    val currentDestinationSnapshot: NavDestination?
        get() = _currentRootDestination.value

    fun attach(controller: NavHostController) {
        navController = controller
    }

    fun detach() {
        navController = null
        rootDestinationByRootId.clear()
        _currentRootDestination.value = null
    }

    /**
     * Register a root graph (identified by [rootGraphId]) with its typed root [destination].
     *
     * Called:
     * - once at start (for the initial root)
     * - later whenever [handleSwitchTo] navigates to another root.
     */
    internal fun registerRootGraph(
        rootGraphId: Int,
        destination: NavDestination
    ) {
        rootDestinationByRootId[rootGraphId] = destination
        _currentRootDestination.update { destination }
    }

    /**
     * Called from [navController.currentBackStackEntryFlow].
     *
     * Maps the current NavDestination ID to its root graph ID and then
     * to the corresponding typed root [NavDestination].
     *
     * This keeps [currentDestinationFlow] in sync for:
     * - system back (hardware, gesture)
     * - navigateUp/popBackStack
     * - explicit navigation.
     */
    internal fun onBackstackDestinationChanged(destinationId: Int) {
        val controller = navController ?: return
        val rootId = rootIdForDestinationId(controller, destinationId) ?: return
        val rootDestination = rootDestinationByRootId[rootId] ?: return

        if (_currentRootDestination.value != rootDestination) {
            _currentRootDestination.update { rootDestination }
        }
    }

    /**
     * Standard in-graph navigation to [navDestination].
     *
     * We do not touch the "root" state here – the root (e.g. Home vs Settings)
     * typically only changes via [handleSwitchTo].
     */
    fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val controller = navController ?: return

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
                        // Hard clear of the entire graph back stack.
                        // Use with care – this discards saved state.
                        val graphId = controller.graph.id
                        popUpTo(graphId) { inclusive = false }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }
        } catch (e: IllegalArgumentException) {
            Logger.e(
                tag = TAG,
                messageString = e.message ?: "Illegal navigation request",
                throwable = e
            )
        } catch (e: Exception) {
            Logger.e(
                tag = TAG,
                messageString = e.message ?: "Unexpected navigation error",
                throwable = e
            )
        }
    }

    /**
     * Switch the active root/tab (e.g. Home <-> Settings).
     *
     * Behavior:
     * - If the requested root is already active, nothing happens.
     * - Otherwise we use a multi-backstack-like pattern:
     *     - popUpTo(navController.graph.startDestinationId) with saveState = true
     *     - launchSingleTop = true
     *     - restoreState = true
     *
     * This lets the navigation system itself restore the last screen
     * inside each tab (e.g. Home-Series instead of always Home-Movies),
     * *provided* you don't break the Home backstack yourself.
     */
    fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        val controller = navController ?: return

        // Already on this root? Do nothing.
        if (_currentRootDestination.value == navDestination) {
            return
        }

        try {
            controller.navigate(navDestination) {
                launchSingleTop = true
                restoreState = true

                // IMPORTANT: use startDestinationId, NOT findStartDestination().id
                // to avoid popping into nested graphs.
                popUpTo(controller.graph.startDestinationId) {
                    saveState = true
                }
            }

            // After navigation, map current destination to its root graph ID.
            val currentNode = controller.currentDestination ?: return
            val rootId = rootIdForDestinationId(controller, currentNode.id) ?: return
            registerRootGraph(rootId, navDestination)
        } catch (e: Exception) {
            Logger.e(
                tag = TAG,
                messageString = e.message ?: "Error while switching tab",
                throwable = e
            )
        }
    }

    /**
     * Pop back in the stack.
     *
     * If [navDestination] is null, behaves like a normal backStack pop.
     * Otherwise, tries to pop back to the given typed destination.
     */
    fun <D : NavDestination> handlePopBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        val controller = navController ?: return

        if (navDestination == null) {
            controller.popBackStack()
        } else {
            val ok = controller.popBackStack(navDestination, inclusive = inclusive)
            if (!ok) {
                controller.popBackStack()
            }
        }
        // Root tracking is updated via currentBackStackEntryFlow.
    }

    /**
     * Convenience wrapper around [NavHostController.navigateUp].
     *
     * As with [handlePopBackTo], root tracking is updated via
     * currentBackStackEntryFlow.
     */
    fun navigateUp() {
        navController?.navigateUp()
    }

    /**
     * Walks up the NavGraph tree to find the top-level child under the NavHost graph
     * for the given [targetId].
     *
     * Example:
     * - Root graph (NavHost) has children:
     *     - HomeGraph (navigation<Home,...>)
     *     - SettingsScreen
     * - Any destination under HomeGraph (Movies, Series, Details, ...) maps to HomeGraph.id.
     * - SettingsScreen maps directly to its own ID.
     */
    private fun rootIdForDestinationId(
        controller: NavHostController,
        targetId: Int
    ): Int? {
        val rootGraph: NavGraph = controller.graph
        val node: AndroidNavDestination = rootGraph.findNode(targetId) ?: return null

        val parent = node.parent ?: return node.id

        if (parent.id == rootGraph.id) {
            // Direct child of the NavHost graph -> root itself.
            return node.id
        }

        var currentParent = parent
        while (currentParent.parent != null && currentParent.parent?.id != rootGraph.id) {
            currentParent = currentParent.parent!!
        }

        return currentParent.id
    }
}
