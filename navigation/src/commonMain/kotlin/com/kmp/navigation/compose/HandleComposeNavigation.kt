package com.kmp.navigation.compose

import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import com.kmp.navigation.DefaultRouteIdProvider
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.RouteIdProvider
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central navigation coordinator used by:
 *  - Navigation implementation (ViewModel-driven navigation)
 *  - Modifier-based navigation helpers.
 *
 * Responsibilities:
 *  - Hold the NavHostController reference.
 *  - Track last visited destination per root graph (sections / tabs).
 *  - Expose the current typed destination as StateFlow for Compose.
 */
internal object HandleComposeNavigation {

    var navController: NavHostController? = null
        private set

    private val routeIdProvider: RouteIdProvider = DefaultRouteIdProvider

    /**
     * Last visited typed destination per root graph (section/tab).
     * Key: root graph id
     * Value: last NavDestination under that root.
     */
    private val lastDestinationByRootId = mutableMapOf<Int, NavDestination>()

    /**
     * Last typed instance seen for a given nav node id.
     * This lets us map NavController.destination.id back to a NavDestination.
     */
    private val lastDestinationByNodeId = mutableMapOf<Int, NavDestination>()

    private val _currentDestinationFlow = MutableStateFlow<NavDestination?>(null)
    val currentDestinationFlow: StateFlow<NavDestination?> = _currentDestinationFlow.asStateFlow()

    val currentDestinationSnapshot: NavDestination?
        get() = _currentDestinationFlow.value

    fun attach(controller: NavHostController) {
        navController = controller
    }

    fun detach() {
        navController = null
        lastDestinationByRootId.clear()
        lastDestinationByNodeId.clear()
        _currentDestinationFlow.value = null
    }

    /**
     * Navigate to a concrete typed [navDestination].
     *
     * Applies [NavOptions] and remembers the destination for:
     *  - its root section
     *  - [currentDestinationFlow]
     */
    fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val controller = navController ?: return

        val targetId = idOf(navDestination)
        val currentId = controller.currentDestination?.id

        // Avoid pushing the same destination again
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

            registerDestinationInstance(navDestination)

        } catch (e: IllegalArgumentException) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    /**
     * Switch to another "tab" / section destination.
     *
     * Behaviour:
     *  - If there is a remembered last destination for that root graph, it navigates
     *    to that child instead of the parent.
     *  - If the effective destination is already current, nothing happens.
     *  - NO popUpTo(findStartDestination()) here, so we do not reset the child
     *    to the initial screen when switching tabs or going back.
     */
    fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        val controller = navController ?: return

        val rootTypeId = idOf(navDestination)
        val rootId = rootIdForDestinationId(rootTypeId)

        val effectiveDestination: NavDestination = if (rootId != null) {
            lastDestinationByRootId[rootId] ?: navDestination
        } else {
            navDestination
        }

        val targetId = idOf(effectiveDestination)
        val currentId = controller.currentDestination?.id

        // Section already points to the same effective destination -> no-op
        if (currentId == targetId) return

        try {
            controller.navigate(effectiveDestination) {
                launchSingleTop = true
                restoreState = true
                // Intentionally no global popUpTo here:
                // we want to preserve the per-section backstack.
            }

            registerDestinationInstance(effectiveDestination)

        } catch (e: Exception) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    /**
     * Pop the back stack to [navDestination] (inclusive or not).
     * If [navDestination] is null, pops one entry.
     *
     * Current destination is updated via [onBackstackDestinationChanged],
     * which listens to NavController's back stack.
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
     * Navigate "up" in the back stack.
     *
     * Again, [currentDestinationFlow] is kept in sync via
     * [onBackstackDestinationChanged].
     */
    fun navigateUp() {
        navController?.navigateUp()
    }

    /**
     * Called from Compose whenever the NavController back stack changes.
     *
     * We map [destinationId] to the last known typed destination instance and
     * update:
     *  - [currentDestinationFlow] (for rememberNavDestination)
     *  - [lastDestinationByRootId] (for switchTab behaviour)
     */
    internal fun onBackstackDestinationChanged(destinationId: Int) {
        val destination = lastDestinationByNodeId[destinationId]
        if (destination != null) {
            _currentDestinationFlow.value = destination
            rootIdForDestinationId(destinationId)?.let { rootId ->
                lastDestinationByRootId[rootId] = destination
            }
        } else {
            // Unknown typed destination (e.g. before first registration).
            _currentDestinationFlow.value = null
        }
    }

    private fun registerDestinationInstance(destination: NavDestination) {
        val destinationId = idOf(destination)
        val destination = lastDestinationByNodeId[destinationId]
        if (destination != null) {
            _currentDestinationFlow.value = destination
            rootIdForDestinationId(destinationId)?.let { rootId ->
                lastDestinationByRootId[rootId] = destination
            }
        } else {
            // Unknown typed destination (e.g. before first registration).
            _currentDestinationFlow.value = null
        }
    }

    /**
     * Returns the ID of the root graph that contains [targetId].
     * Used for section/tab behaviour.
     */
    private fun rootIdForDestinationId(targetId: Int): Int? {
        val controller = navController ?: return null
        val rootGraph = controller.graph
        val node = rootGraph.findNode(targetId) ?: return null

        val parent = node.parent ?: return node.id

        // Direct child of the root?
        if (parent.id == rootGraph.id) {
            return node.id
        }

        // Otherwise walk up until the direct child of the root graph is reached
        var currentParent = parent
        while (currentParent.parent != null && currentParent.parent?.id != rootGraph.id) {
            currentParent = currentParent.parent!!
        }

        return currentParent.id
    }

    private fun <D : NavDestination> idOf(destination: D): Int =
        destination::class.routeId()

    private fun <D : NavDestination> KClass<D>.routeId(): Int =
        routeIdProvider.idFor(this)
}
