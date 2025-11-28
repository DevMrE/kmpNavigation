package com.kmp.navigation.compose

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import com.kmp.navigation.DefaultRouteIdProvider
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.RouteIdProvider
import com.kmp.navigation.compose.HandleComposeNavigation.currentDestinationFlow
import com.kmp.navigation.compose.HandleComposeNavigation.handleNavigateTo
import com.kmp.navigation.compose.HandleComposeNavigation.handlePopBackTo
import com.kmp.navigation.compose.HandleComposeNavigation.handleSwitchTo
import com.kmp.navigation.compose.HandleComposeNavigation.lastDestinationByRootId
import com.kmp.navigation.compose.HandleComposeNavigation.navigateUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/**
 * Central navigation coordinator used by the KMP navigation library.
 *
 * Responsibilities:
 * - Holds the current [NavHostController] for Compose Navigation.
 * - Converts a [NavDestination] (your typed destination) into an internal route ID via [RouteIdProvider].
 * - Applies navigation options ([NavOptions]) such as `singleTop`, `restoreState` and back stack behavior.
 * - Remembers, per root graph (section), the last visited destination to support proper tab-switch behavior.
 * - Exposes a [currentDestinationFlow] that reflects the last destination this handler navigated to.
 *
 * This object is internal to the library and is used by:
 * - [NavigationImpl] (for ViewModel-based navigation).
 * - Modifier helpers (e.g. `Modifier.navigateTo`, `Modifier.switchTab`) if you use them.
 */
internal object HandleComposeNavigation {

    /**
     * The active [NavHostController] managed by this handler.
     *
     * It is attached in `RegisterNavigation` (via `rememberMutableComposeNavigation`)
     * and detached when the corresponding `NavHost` leaves composition.
     */
    var navController: NavHostController? = null
        private set

    private val routeIdProvider: RouteIdProvider = DefaultRouteIdProvider

    /**
     * For each root graph (tab), remembers the most recently visited destination under that root.
     *
     * Key: root navigation graph ID (as seen by Compose Navigation)
     * Value: last [NavDestination] visited under that graph
     *
     * This is what enables:
     * - `switchTab(Home)` to return you to the last screen in the "Home" section
     *   instead of always going back to the initial screen.
     */
    private val lastDestinationByRootId = mutableMapOf<Int, NavDestination>()

    /**
     * Backing state for [currentDestinationFlow].
     *
     * This is updated whenever navigation is performed via this handler:
     * - [handleNavigateTo]
     * - [handleSwitchTo]
     * - [handlePopBackTo]
     * - [navigateUp]
     *
     * It intentionally only tracks destinations navigated by the library itself,
     * not arbitrary manual calls on the [NavHostController] outside of this object.
     */
    private val _currentDestinationFlow = MutableStateFlow<NavDestination?>(null)

    /**
     * Public read-only stream of the logical "current" [NavDestination].
     *
     * Typical usage is via a helper such as:
     *
     * ```kotlin
     * @Composable
     * fun rememberCurrentDestination(): NavDestination? {
     *     val navigation = rememberNavigation()
     *     return navigation.currentDestination.collectAsState().value
     * }
     * ```
     *
     * Notes:
     * - The value is updated when navigation is triggered through this navigation library
     *   (ViewModel `Navigation`, modifier helpers, etc.).
     * - If you directly manipulate the underlying [NavHostController] yourself
     *   (e.g. custom `popBackStack`), this flow will not know about it.
     */
    val currentDestinationFlow: StateFlow<NavDestination?> get() = _currentDestinationFlow

    /**
     * Attach a [NavHostController] to this handler.
     *
     * Called from the library's `RegisterNavigation` when a NavHost is created.
     */
    fun attach(controller: NavHostController) {
        navController = controller
        // We only know about logical destinations once the user navigates via this handler,
        // so we do not try to infer a NavDestination here.
    }

    /**
     * Detach the current [NavHostController] and reset in-memory navigation state.
     *
     * Called from `DisposableEffect` in `rememberMutableComposeNavigation` when the NavHost
     * leaves composition.
     */
    fun detach() {
        navController = null
        lastDestinationByRootId.clear()
        _currentDestinationFlow.value = null
    }

    /**
     * Navigate to [navDestination], applying the given [options].
     *
     * - Respects [NavOptions.singleTop], [NavOptions.restoreState] and back stack options.
     * - Skips navigation if the target destination is already the current destination.
     * - Updates [lastDestinationByRootId] and [currentDestinationFlow] after a successful navigation.
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
                        // Clear everything up to the graph root
                        controller.graph.id.let { graphId ->
                            popUpTo(graphId) { inclusive = false }
                        }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }

            rootIdForDestinationId(targetId)?.let { rootId ->
                lastDestinationByRootId[rootId] = navDestination
            }

            _currentDestinationFlow.value = navDestination

        } catch (e: IllegalArgumentException) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    /**
     * Switch to another “tab” or root destination, restoring the last screen within that section.
     *
     * Behavior:
     * - If this root section has been visited before, navigates to the last destination under that root.
     * - If not, navigates to [navDestination] itself (the initial screen of that section).
     * - Uses the standard bottom-navigation pattern:
     *   - `launchSingleTop = true`
     *   - `restoreState = true`
     *   - `popUpTo(startDestination) { saveState = true }`
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

        // Already on this destination → nothing to do
        if (currentId == targetId) return

        try {
            controller.navigate(effectiveDestination) {
                launchSingleTop = true
                restoreState = true

                popUpTo(controller.graph.findStartDestination().id) {
                    saveState = true
                }
            }

            rootIdForDestinationId(targetId)?.let { resolvedRootId ->
                lastDestinationByRootId[resolvedRootId] = effectiveDestination
            }

            _currentDestinationFlow.value = effectiveDestination

        } catch (e: Exception) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    /**
     * Pop the back stack.
     *
     * - If [navDestination] is `null`, behaves like a plain `popBackStack()` on the controller.
     * - If [navDestination] is not `null`, pops back to that destination (inclusive or not).
     * - If the requested pop fails, falls back to a simple `popBackStack()`.
     *
     * After the pop, [currentDestinationFlow] is set to the best-known logical destination.
     * This is a best-effort update: we rely on the internal maps and may not always perfectly
     * reconstruct a historical destination instance.
     */
    fun <D : NavDestination> handlePopBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        val controller = navController ?: return

        if (navDestination == null) {
            val popped = controller.popBackStack()
            if (popped) {
                updateCurrentFromController()
            }
            return
        }

        val ok = controller.popBackStack(navDestination, inclusive = inclusive)
        if (!ok) {
            controller.popBackStack()
        }

        updateCurrentFromController()
    }

    /**
     * Navigate one step up in the back stack.
     *
     * This simply delegates to `NavHostController.navigateUp()` and then
     * tries to update [currentDestinationFlow] based on the new
     * controller state.
     */
    fun navigateUp() {
        val controller = navController ?: return
        val didNavigateUp = controller.navigateUp()
        if (didNavigateUp) {
            updateCurrentFromController()
        }
    }

    /**
     * Resolve the ID of the root navigation graph that contains the destination with [targetId].
     *
     * This is used to group destinations into "sections" (e.g. bottom bar tabs).
     *
     * Returns:
     * - The ID of the direct child of the root graph that ultimately contains [targetId].
     * - `null` if no such node can be found.
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

        // Otherwise walk upwards until we reach a direct child of the root graph
        var currentParent = parent
        while (currentParent.parent != null && currentParent.parent?.id != rootGraph.id) {
            currentParent = currentParent.parent!!
        }

        return currentParent.id
    }

    /**
     * Compute a stable internal ID for a [NavDestination] using the [RouteIdProvider].
     */
    private fun <D : NavDestination> idOf(destination: D): Int =
        destination::class.routeId()

    /**
     * Convert a [KClass] of a [NavDestination] into a route ID.
     */
    private fun <D : NavDestination> KClass<D>.routeId(): Int =
        routeIdProvider.idFor(this)

    /**
     * Best-effort update of [currentDestinationFlow] after a back operation.
     *
     * We look at the current controller destination and try to find the
     * last known logical [NavDestination] that matches its ID, falling
     * back to the current value if necessary.
     */
    private fun updateCurrentFromController() {
        val controller = navController ?: return
        val currentId = controller.currentDestination?.id ?: run {
            _currentDestinationFlow.value = null
            return
        }

        val rootId = rootIdForDestinationId(currentId)
        val candidate = if (rootId != null) {
            lastDestinationByRootId[rootId]
        } else {
            null
        }

        _currentDestinationFlow.value = candidate ?: _currentDestinationFlow.value
    }
}
