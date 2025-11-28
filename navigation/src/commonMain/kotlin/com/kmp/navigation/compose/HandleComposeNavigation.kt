package com.kmp.navigation.compose

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import com.kmp.navigation.DefaultRouteIdProvider
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.RouteIdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/**
 * Internal navigation orchestration for Compose.
 *
 * Responsibilities:
 * - Holds the NavHostController reference.
 * - Implements Navigation operations (navigateTo, switchTab, popBackTo, navigateUp).
 * - Tracks the last typed NavDestination per nav-graph node id and per root section.
 * - Exposes [currentDestinationFlow] so UI can observe the active destination via
 *   [rememberNavDestination].
 */
internal object HandleComposeNavigation {

    var navController: NavHostController? = null
        private set

    private val routeIdProvider: RouteIdProvider = DefaultRouteIdProvider

    /**
     * Last known typed destination per NavGraph node id (NavDestination.id).
     *
     * Key: NavGraph node id from [androidx.navigation.NavDestination.id].
     * Value: Typed [NavDestination] instance we navigated to most recently for that node.
     */
    private val lastDestinationByNodeId = mutableMapOf<Int, NavDestination>()

    /**
     * Last known typed destination per root graph.
     *
     * Root graphs are direct children of the NavHost graph (e.g. Home, Settings).
     *
     * Key: root NavGraph node id
     * Value: last typed [NavDestination] that lived in this root graph
     */
    private val lastDestinationByRootId = mutableMapOf<Int, NavDestination>()

    /**
     * Currently active typed destination as seen by our navigation layer.
     *
     * This is updated whenever:
     * - we call [handleNavigateTo] or [handleSwitchTo], and
     * - the NavHost backstack changes (back gesture, system back, deep link)
     *   via [onBackstackDestinationChanged].
     */
    private val _currentDestinationFlow = MutableStateFlow<NavDestination?>(null)
    val currentDestinationFlow: StateFlow<NavDestination?> get() = _currentDestinationFlow
    val currentDestinationSnapshot: NavDestination? get() = _currentDestinationFlow.value

    fun attach(controller: NavHostController) {
        navController = controller
        lastDestinationByNodeId.clear()
        lastDestinationByRootId.clear()
        _currentDestinationFlow.value = null
    }

    fun detach() {
        navController = null
        lastDestinationByNodeId.clear()
        lastDestinationByRootId.clear()
        _currentDestinationFlow.value = null
    }

    /**
     * Called from [NavHostController.currentBackStackEntryFlow].
     *
     * [destinationId] must be the `id` of the *current* NavGraph node.
     * We use the previously registered [lastDestinationByNodeId] map to
     * recover a typed [NavDestination] instance for that node.
     *
     * This is how OS-driven navigation (back gestures, system back, deep links)
     * is mirrored into our typed world.
     */
    fun onBackstackDestinationChanged(destinationId: Int) {
        val destination = lastDestinationByNodeId[destinationId]
        if (destination != null) {
            _currentDestinationFlow.value = destination

            rootIdForDestinationId(destinationId)?.let { rootId ->
                lastDestinationByRootId[rootId] = destination
            }
        } else {
            // We do not know a typed instance for this NavGraph node yet
            // (e.g. initial destination created by NavHost itself).
            // To avoid UI flicker, we keep the previous current destination
            // instead of resetting everything to null.
        }
    }

    /**
     * Push a typed destination onto the current backstack.
     *
     * Honors [NavOptions] (singleTop, restoreState, PopTo, Clear).
     */
    fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val controller = navController ?: return

        val targetTypeNodeId = idOf(navDestination)
        val currentNodeId = controller.currentDestination?.id

        // If the current NavGraph node already corresponds to this type,
        // skip duplicate navigation.
        if (currentNodeId == targetTypeNodeId) return

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
                        // Clear the entire NavHost backstack (up to the root graph).
                        controller.graph.id.let { graphId ->
                            popUpTo(graphId) { inclusive = false }
                        }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }

            // After navigate() the NavController already points at the new entry.
            registerDestinationInstance(navDestination)

        } catch (e: IllegalArgumentException) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    /**
     * Switch between *root sections* (e.g. bottom navigation items).
     *
     * This follows the official multi-backstack recipe:
     * - popUpTo(startDestination) { saveState = true } – keep each root stack alive
     * - restoreState = true – restore the last stack when revisiting a root
     * - launchSingleTop = true – avoid duplicate root entries.
     *
     * For the very first visit of a root, the current destination will be exactly
     * [navDestination] and we register it manually.
     *
     * For subsequent visits, NavController may restore a child (e.g. Series instead
     * of the parent Home graph). In that case, the real destination flows through
     * [onBackstackDestinationChanged] and updates our state correctly.
     */
    fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        val controller = navController ?: return

        val rootTypeId = idOf(navDestination)
        val rootGraphId = rootIdForDestinationId(rootTypeId)
        val hasHistory = rootGraphId != null && lastDestinationByRootId.containsKey(rootGraphId)

        try {
            controller.navigate(navDestination) {
                launchSingleTop = true
                restoreState = true

                // Standard bottom-nav pattern: keep separate backstacks per root graph.
                popUpTo(controller.graph.findStartDestination().id) {
                    saveState = true
                }
            }

            // First time we ever enter this root: NavController cannot restore any state,
            // so the current destination is the root itself and we can safely register it.
            if (!hasHistory) {
                registerDestinationInstance(navDestination)
            }

            // For subsequent visits, `currentBackStackEntryFlow` will emit the restored
            // child destination and we update state in onBackstackDestinationChanged().
        } catch (e: Exception) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    /**
     * Pop the backstack either by:
     * - a specific typed destination (if provided), or
     * - a single step (if [navDestination] is null).
     *
     * The resulting destination will be observed via
     * [onBackstackDestinationChanged] and synchronized with our state.
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
            if (!ok) controller.popBackStack()
        }
    }

    /**
     * Delegate to [NavHostController.navigateUp]. The resulting backstack change will
     * be seen by [onBackstackDestinationChanged].
     */
    fun navigateUp() {
        navController?.navigateUp()
    }

    /**
     * Register a typed [destination] for the *current* NavGraph node id.
     *
     * Called after we ourselves navigated (navigateTo / first switchTab).
     */
    private fun <D : NavDestination> registerDestinationInstance(destination: D) {
        val controller = navController ?: return
        val currentNodeId = controller.currentDestination?.id ?: return

        // Remember typed instance for this NavGraph node
        lastDestinationByNodeId[currentNodeId] = destination

        // Also remember "last destination for this root section"
        rootIdForDestinationId(currentNodeId)?.let { rootId ->
            lastDestinationByRootId[rootId] = destination
        }

        _currentDestinationFlow.value = destination
    }

    /**
     * Find the id of the *root graph* that contains the node with [targetId].
     *
     * Example graph:
     * - NavHost graph
     *   - Home (nested graph, id = X)
     *     - Movies
     *     - Series
     *   - Settings
     *
     * For Movies/Series, this returns X (Home); for Home, it returns X;
     * for Settings, it returns Settings.id.
     */
    private fun rootIdForDestinationId(targetId: Int): Int? {
        val controller = navController ?: return null
        val rootGraph = controller.graph

        val node = rootGraph.findNode(targetId) ?: return null
        val parent = node.parent ?: return node.id

        // Direct child of the NavHost root
        if (parent.id == rootGraph.id) {
            return node.id
        }

        // Otherwise walk up until the direct child of the NavHost root is reached
        var currentParent = parent
        while (currentParent.parent != null && currentParent.parent?.id != rootGraph.id) {
            currentParent = currentParent.parent!!
        }
        return currentParent.id
    }

    private fun <D : NavDestination> idOf(destination: D): Int =
        routeIdProvider.idFor(destination::class)

    @Suppress("unused")
    private fun <D : NavDestination> KClass<D>.routeId(): Int =
        routeIdProvider.idFor(this)
}
