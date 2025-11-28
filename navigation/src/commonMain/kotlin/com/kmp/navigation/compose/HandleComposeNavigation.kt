package com.kmp.navigation.compose

import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import com.kmp.navigation.DefaultRouteIdProvider
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.RouteIdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Internal navigation hub used by:
 * - ViewModel-driven navigation (via NavigationImpl)
 * - Modifier extensions (navigateTo, switchTab, navigateUp, popBackTo)
 *
 * Responsibilities:
 * - holds the NavHostController
 * - tracks the last visited destination per "root graph" (section)
 * - exposes a StateFlow with the current typed NavDestination
 */
internal object HandleComposeNavigation {

    var navController: NavHostController? = null
        private set

    private val routeIdProvider: RouteIdProvider = DefaultRouteIdProvider

    /**
     * Map of all typed destinations we ever navigated to.
     * Key: destination id (NavDestination hash)
     * Value: last typed NavDestination instance for that id.
     */
    private val destinationById = mutableMapOf<Int, NavDestination>()

    /**
     * Last destination per root graph (section).
     * Key: root navigation graph id
     * Value: last typed NavDestination under that root.
     */
    private val lastDestinationByRootId = mutableMapOf<Int, NavDestination>()

    /**
     * Current typed destination as seen by consumers (TopBar, BottomBar, etc.).
     */
    private val _currentDestination = MutableStateFlow<NavDestination?>(null)
    val currentDestinationFlow: StateFlow<NavDestination?> = _currentDestination.asStateFlow()
    val currentDestinationSnapshot: NavDestination? get() = _currentDestination.value

    fun attach(controller: NavHostController) {
        navController = controller
    }

    fun detach() {
        navController = null
        destinationById.clear()
        lastDestinationByRootId.clear()
        _currentDestination.value = null
    }

    /**
     * Called from rememberMutableComposeNavigation whenever the NavController
     * backstack changes (OS back, BackHandler, system gestures, etc.).
     *
     * We only trust ids that we know (destinationById).
     */
    fun onBackstackDestinationChanged(destinationId: Int) {
        val controller = navController ?: return

        val destination = destinationById[destinationId] ?: return
        _currentDestination.value = destination

        val rootId = rootIdForDestinationId(controller, destinationId)
        if (rootId != null) {
            lastDestinationByRootId[rootId] = destination
        }
    }

    fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val controller = navController ?: return

        val targetId = idOf(navDestination)
        val currentId = controller.currentDestination?.id

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
                        val graphId = controller.graph.id
                        popUpTo(graphId) { inclusive = false }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }

            updateTrackingAfterNavigation(controller, targetId, navDestination)
        } catch (e: IllegalArgumentException) {
            Logger.e(
                tag = "Navigation",
                messageString = e.message ?: "IllegalArgumentException in handleNavigateTo",
                throwable = e
            )
        }
    }

    /**
     * Section-aware tab switch:
     * - If the requested section is already active, we either:
     *   - do nothing (if already on the last destination of that section), or
     *   - pop back to that destination if it exists in the backstack.
     * - If it is a different section, we first try to pop back to the last
     *   known destination for that section. If it is not in the backstack,
     *   we navigate once.
     */
    fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        val controller = navController ?: return

        val requestedId = idOf(navDestination)
        val requestedRootId = rootIdForDestinationId(controller, requestedId)

        val currentId = controller.currentDestination?.id
        val currentRootId = currentId?.let { rootIdForDestinationId(controller, it) }

        // Destination we actually want to show for that section (tab)
        val effectiveDestination: NavDestination = if (requestedRootId != null) {
            lastDestinationByRootId[requestedRootId] ?: navDestination
        } else {
            navDestination
        }

        val effectiveId = idOf(effectiveDestination)

        // Case 1: already in this section
        if (requestedRootId != null && currentRootId == requestedRootId) {
            // If we are already on the correct destination: nothing to do
            if (currentId == effectiveId) {
                // Make sure state flow is in sync
                controller.currentDestination?.id?.let { id ->
                    destinationById[id]?.let { _currentDestination.value = it }
                }
                return
            }

            // We are in the same section but on a different screen.
            // Try to pop back to the last known destination instead of pushing.
            val popped = controller.popBackStack(effectiveDestination, inclusive = false)
            if (popped) {
                updateTrackingAfterNavigation(controller, effectiveId, effectiveDestination)
                return
            }

            // Not in backstack -> navigate once.
            navigateAndTrack(controller, effectiveDestination, effectiveId)
            return
        }

        // Case 2: coming from a different section or a leaf without section
        // Try to pop back to the last known destination of that section.
        val popped = controller.popBackStack(effectiveDestination, inclusive = false)
        if (popped) {
            updateTrackingAfterNavigation(controller, effectiveId, effectiveDestination)
            return
        }

        // Not in backstack -> navigate once.
        navigateAndTrack(controller, effectiveDestination, effectiveId)
    }

    fun <D : NavDestination> handlePopBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        val controller = navController ?: return

        if (navDestination == null) {
            val popped = controller.popBackStack()
            if (popped) {
                controller.currentDestination?.id?.let { onBackstackDestinationChanged(it) }
            }
        } else {
            val ok = controller.popBackStack(navDestination, inclusive = inclusive)
            if (ok) {
                controller.currentDestination?.id?.let { onBackstackDestinationChanged(it) }
            } else {
                val popped = controller.popBackStack()
                if (popped) {
                    controller.currentDestination?.id?.let { onBackstackDestinationChanged(it) }
                }
            }
        }
    }

    fun navigateUp() {
        val controller = navController ?: return
        val didNavigate = controller.navigateUp()
        if (didNavigate) {
            controller.currentDestination?.id?.let { onBackstackDestinationChanged(it) }
        }
    }

    private fun navigateAndTrack(
        controller: NavHostController,
        destination: NavDestination,
        destinationId: Int
    ) {
        try {
            controller.navigate(destination) {
                launchSingleTop = true
                // no popUpTo here: we keep section-specific history
            }
            updateTrackingAfterNavigation(controller, destinationId, destination)
        } catch (e: Exception) {
            Logger.e(
                tag = "Navigation",
                messageString = e.message ?: "Navigation error in handleSwitchTo",
                throwable = e
            )
        }
    }

    private fun updateTrackingAfterNavigation(
        controller: NavHostController,
        destinationId: Int,
        destination: NavDestination
    ) {
        destinationById[destinationId] = destination

        val rootId = rootIdForDestinationId(controller, destinationId)
        if (rootId != null) {
            lastDestinationByRootId[rootId] = destination
        }

        _currentDestination.value = destination
    }

    /**
     * Returns the id of the "root graph" (direct child of the NavHost graph)
     * that contains the destination with [targetId].
     */
    private fun rootIdForDestinationId(
        controller: NavHostController,
        targetId: Int
    ): Int? {
        val rootGraph = controller.graph
        val node = rootGraph.findNode(targetId) ?: return null

        val parent = node.parent ?: return node.id

        // Direct child of the root?
        if (parent.id == rootGraph.id) {
            return node.id
        }

        // Walk up the hierarchy until we reach a direct child of the root graph
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
