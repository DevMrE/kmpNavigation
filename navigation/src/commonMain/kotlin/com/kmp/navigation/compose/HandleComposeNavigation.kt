package com.kmp.navigation.compose

import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination as AndroidNavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import co.touchlab.kermit.Logger
import com.kmp.navigation.DefaultRouteIdProvider
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.RouteIdProvider
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Central, shared navigation state for the Compose layer.
 *
 * Responsibilities:
 * - Holds the [NavHostController] reference (attached from the Composable world).
 * - Tracks the current typed [NavDestination] as Compose state.
 * - Keeps a lightweight map from "typed route id" -> last seen [NavDestination] instance,
 *   so we can reconstruct the current destination when the back stack changes.
 *
 * Important:
 * - The real source of truth for "where we are" is always [NavHostController].
 *   We only mirror its [currentBackStackEntryFlow] into [currentDestinationFlow].
 */
internal object HandleComposeNavigation {

    private val logger = Logger.withTag("Navigation")

    /**
     * Reference to the NavHostController used by RegisterNavigation.
     * Attached from rememberMutableComposeNavigation.
     */
    var navController: NavHostController? = null
        private set

    private val routeIdProvider: RouteIdProvider = DefaultRouteIdProvider

    /**
     * Map from route id (Int, same id that Navigation-Compose uses for typed routes)
     * to the last typed [NavDestination] instance we navigated to.
     *
     * This lets us reconstruct the typed destination from a [NavDestination.id]
     * when the back stack changes (including OS back gesture).
     */
    private val destinationById = mutableMapOf<Int, NavDestination>()

    /**
     * Compose state of the "current typed destination" as far as we know.
     * Backed by [currentDestinationState].
     */
    private val currentDestinationState = MutableStateFlow<NavDestination?>(null)

    val currentDestinationFlow: StateFlow<NavDestination?>
        get() = currentDestinationState

    val currentDestinationSnapshot: NavDestination?
        get() = currentDestinationState.value

    /**
     * Called from rememberMutableComposeNavigation when the controller is ready.
     */
    fun attach(controller: NavHostController) {
        navController = controller
    }

    /**
     * Called from rememberMutableComposeNavigation on disposal of the NavHost.
     * Clears internal caches.
     */
    fun detach() {
        navController = null
        destinationById.clear()
        currentDestinationState.value = null
    }

    /**
     * Called once from RegisterNavigation to register the typed start destination.
     * This is needed because the initial backstack entry is created by NavHost,
     * not via our [navigateTo] call.
     */
    fun registerStartDestination(startDestination: NavDestination) {
        val id = idOf(startDestination)
        destinationById[id] = startDestination
        // currentDestinationState wird durch currentBackStackEntryFlow gesetzt,
        // sobald der NavHost läuft. Hier NICHT manuell forcieren.
    }

    /**
     * Mirrors NavController.currentBackStackEntryFlow into our typed state.
     * Called from rememberMutableComposeNavigation.
     */
    fun onBackstackDestinationChanged(destinationId: Int) {
        val controller = navController ?: return

        val typed = destinationById[destinationId]
        if (typed != null) {
            currentDestinationState.value = typed
            return
        }

        // Wir kennen die Instanz nicht (z.B. nie via navigateTo aufgerufen).
        // In diesem Fall loggen wir nur – die UI kann mit initialDestination
        // in rememberNavDestination arbeiten.
        logger.d { "Unknown typed destination id=$destinationId; no cached instance." }

        // currentDestinationState bleibt unverändert, damit rememberNavDestination(initial)
        // einen stabilen Fallback hat.
    }

    /**
     * Navigate to any typed NavDestination.
     *
     * - No-op if we're already on the same destination id.
     * - Mirrors options like singleTop, restoreState & our own backstack options.
     * - The actual "current destination" update happens via onBackstackDestinationChanged.
     */
    fun <D : NavDestination> handleNavigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val controller = navController ?: return

        val targetId = idOf(navDestination)
        val currentId = controller.currentDestination?.id

        if (currentId == targetId) {
            return
        }

        // Cache typed instance so onBackstackDestinationChanged can resolve it
        destinationById[targetId] = navDestination

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
                        // Clear everything above the root graph
                        popUpTo(controller.graph.id) {
                            inclusive = false
                        }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }
        } catch (e: IllegalArgumentException) {
            logger.e(e) { "navigateTo(${navDestination::class.simpleName}) failed: ${e.message}" }
        }
    }

    /**
     * Switch to a top-level section/tab.
     *
     * Behaviour:
     * - If the requested section is already active (same root graph), do nothing.
     * - Otherwise use the official bottom-nav pattern:
     *   popUpTo(rootStart) { saveState = true }, launchSingleTop, restoreState = true.
     *
     * We rely on Navigation-Compose's save/restoreState to bring back the
     * previous child destination (e.g. Home-Series vs Home-Movies).
     */
    fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        val controller = navController ?: return

        val targetId = idOf(navDestination)
        val currentId = controller.currentDestination?.id

        // Already in the requested section? -> no-op
        if (currentId != null && isSameRootSection(controller, currentId, targetId)) {
            return
        }

        // Cache the root destination itself, so that the next backstack change
        // can resolve it even if we never navigated there before.
        destinationById[targetId] = navDestination

        try {
            controller.navigate(navDestination) {
                // Official multiple-backstack pattern
                launchSingleTop = true
                restoreState = true

                popUpTo(controller.graph.findStartDestination().id) {
                    saveState = true
                }
            }
            // KEIN manuelles currentDestinationState-Update;
            // das macht onBackstackDestinationChanged.
        } catch (e: Exception) {
            logger.e(e) { "switchTab(${navDestination::class.simpleName}) failed: ${e.message}" }
        }
    }

    /**
     * Pop within the backstack. We let the NavController drive the state,
     * and only mirror the new destination when currentBackStackEntryFlow fires.
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

        val ok = controller.popBackStack(navDestination, inclusive = inclusive)
        if (!ok) controller.popBackStack()
        // State wird über onBackstackDestinationChanged aktualisiert.
    }

    fun navigateUp() {
        navController?.navigateUp()
        // State wird über onBackstackDestinationChanged aktualisiert.
    }

    /**
     * Returns true if [currentDestinationId] and [targetDestinationId] belong
     * to the same top-level root graph (e.g. same "section").
     *
     * This is what we use to prevent redundant switchTab navigations.
     */
    private fun isSameRootSection(
        controller: NavHostController,
        currentDestinationId: Int,
        targetDestinationId: Int
    ): Boolean {
        val currentRoot = rootIdForDestinationId(controller, currentDestinationId)
        val targetRoot = rootIdForDestinationId(controller, targetDestinationId) ?: targetDestinationId

        return currentRoot != null && currentRoot == targetRoot
    }

    /**
     * Given a destination id, walk up the NavGraph parents until we hit a
     * direct child of the NavHost root. That child is considered the "root section".
     */
    private fun rootIdForDestinationId(
        controller: NavHostController,
        targetId: Int
    ): Int? {
        val rootGraph: NavGraph = controller.graph
        val node: AndroidNavDestination = rootGraph.findNode(targetId) ?: return null

        val parent = node.parent ?: return node.id

        // Direct child of the root?
        if (parent.id == rootGraph.id) {
            return node.id
        }

        // Otherwise climb up until we reach a direct child of the root graph
        var currentParent = parent
        while (currentParent.parent != null && currentParent.parent?.id != rootGraph.id) {
            currentParent = currentParent.parent!!
        }

        return currentParent.id
    }

    private fun <D : NavDestination> idOf(destination: D): Int =
        routeIdProvider.idFor(destination::class as KClass<out NavDestination>)
}
