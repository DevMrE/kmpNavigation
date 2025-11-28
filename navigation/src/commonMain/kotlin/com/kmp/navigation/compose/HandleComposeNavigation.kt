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
 * Central navigation handler used internally by the KMP navigation layer.
 *
 * Responsibilities:
 * - Hold the current [NavHostController] instance.
 * - Provide `navigateTo`, `switchTo`, `popBackTo`, `navigateUp` semantics.
 * - Remember the last visited destination per root graph (for tab-like behavior).
 * - Expose a Compose-friendly stream of the current [NavDestination].
 * - React to back stack changes driven by the NavController itself
 *   (system back, gestures, `popBackStack`, etc.).
 */
internal object HandleComposeNavigation {

    var navController: NavHostController? = null
        private set

    private val routeIdProvider: RouteIdProvider = DefaultRouteIdProvider

    /**
     * Per root graph, remembers the last leaf destination navigated to via this handler.
     *
     * Key: root navigation graph ID
     * Value: last [NavDestination] under that root.
     */
    private val lastDestinationByRootId = mutableMapOf<Int, NavDestination>()

    /**
     * Current destination as seen by the navigation layer.
     *
     * - Updated when we navigate via [handleNavigateTo] or [handleSwitchTo].
     * - Kept in sync (best-effort) with back stack changes via
     *   [onBackstackDestinationChanged].
     */
    private val _currentDestination = MutableStateFlow<NavDestination?>(null)

    val currentDestinationFlow: StateFlow<NavDestination?>
        get() = _currentDestination

    val currentDestinationSnapshot: NavDestination?
        get() = _currentDestination.value

    /**
     * Attach a [NavHostController] to this handler.
     *
     * Called from the Compose side when the NavHost is created.
     */
    fun attach(controller: NavHostController) {
        navController = controller
    }

    /**
     * Detach the current [NavHostController] and clear all cached state.
     *
     * Called when the NavHost is disposed.
     */
    fun detach() {
        navController = null
        lastDestinationByRootId.clear()
        _currentDestination.value = null
    }

    /**
     * Navigate to the given [navDestination].
     *
     * - Avoids duplicate navigation if the target is already the current destination.
     * - Applies [NavOptions] (singleTop, restoreState, backstack strategy).
     * - Updates the "last destination per root graph" cache and current destination.
     */
    fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val controller = navController ?: return

        val targetId = idOf(navDestination)
        val currentId = controller.currentDestination?.id

        // Avoid re-navigating to the same destination
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
                        // Clear back stack up to the graph root (non-inclusive)
                        controller.graph.id.let { graphId ->
                            popUpTo(graphId) { inclusive = false }
                        }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }

            // Track last destination for its root graph (tab / section behavior)
            rootIdForDestinationId(targetId)?.let { rootId ->
                lastDestinationByRootId[rootId] = navDestination
            }

            // Update current destination with the typed instance
            _currentDestination.value = navDestination

        } catch (e: IllegalArgumentException) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    /**
     * Switch to a "tab-like" destination.
     *
     * Semantics:
     * - If there is a remembered destination for the same root graph, we restore that.
     * - Otherwise we navigate directly to [navDestination].
     * - Uses `launchSingleTop` and `restoreState` and pops up to the graph's start
     *   (with `saveState = true`) to get standard multi-backstack behavior.
     */
    fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        val controller = navController ?: return

        val rootTypeId = idOf(navDestination)
        val rootId = rootIdForDestinationId(rootTypeId)

        // Decide which destination should actually be used for this root:
        //  - If we have a previously stored leaf for this root, use it.
        //  - Otherwise fall back to the passed navDestination.
        val effectiveDestination: NavDestination = if (rootId != null) {
            lastDestinationByRootId[rootId] ?: navDestination
        } else {
            navDestination
        }

        val targetId = idOf(effectiveDestination)
        val currentId = controller.currentDestination?.id

        // Avoid re-navigating to the same destination
        if (currentId == targetId) return

        try {
            controller.navigate(effectiveDestination) {
                launchSingleTop = true
                restoreState = true

                // Standard bottom-nav pattern: keep separate backstacks per root graph
                popUpTo(controller.graph.findStartDestination().id) {
                    saveState = true
                }
            }

            // Update "last destination for this root" after switching.
            rootIdForDestinationId(targetId)?.let { resolvedRootId ->
                lastDestinationByRootId[resolvedRootId] = effectiveDestination
            }

            // Current destination is now the effective one
            _currentDestination.value = effectiveDestination

        } catch (e: Exception) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    /**
     * Pop the back stack to [navDestination] or just one level if it's null.
     *
     * Used by higher-level `popBackTo` APIs and by system back handling glue.
     *
     * The actual current destination will be updated via
     * [onBackstackDestinationChanged] through the NavController's
     * [androidx.navigation.NavController.currentBackStackEntryFlow].
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
     * Navigate "up" in the back stack, delegating to the NavController.
     *
     * As with [handlePopBackTo], the current destination is synchronized via
     * [onBackstackDestinationChanged].
     */
    fun navigateUp() {
        navController?.navigateUp()
    }

    /**
     * Keep our internal state in sync with the actual NavController back stack
     * when it changes from outside this handler:
     *
     * - system back button
     * - back gestures
     * - direct `navController.popBackStack()` calls
     *
     * Because we only get a destination ID (no typed [NavDestination] instance),
     * we cannot reconstruct a full typed destination with arguments in the general case.
     *
     * Strategy:
     * - If the ID matches the current typed destination, keep it as is.
     * - Otherwise:
     *   - Resolve the root graph for [destinationId].
     *   - If our cached destination for that root has the same ID, reuse it.
     *   - If not, clear the cache for that root and set the current destination to `null`.
     */
    fun onBackstackDestinationChanged(destinationId: Int) {
        val current = _currentDestination.value
        if (current != null && idOf(current) == destinationId) {
            // Already in sync
            return
        }

        val rootId = rootIdForDestinationId(destinationId)
        if (rootId == null) {
            _currentDestination.value = null
            return
        }

        val existing = lastDestinationByRootId[rootId]
        if (existing != null && idOf(existing) == destinationId) {
            _currentDestination.value = existing
            return
        }

        // We no longer have a reliable typed instance for this ID.
        lastDestinationByRootId.remove(rootId)
        _currentDestination.value = null
    }

    /**
     * Resolve the "root graph id" for a given destination ID.
     *
     * A root graph is the direct child of the NavController's graph. We walk
     * up the parent chain until we reach that direct child.
     */
    private fun rootIdForDestinationId(targetId: Int): Int? {
        val controller = navController ?: return null
        val rootGraph = controller.graph
        val node = rootGraph.findNode(targetId) ?: return null

        val parent = node.parent ?: return node.id

        // Direct child of the root graph?
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
