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
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

internal object HandleComposeNavigation {

    var navController: NavHostController? = null
        private set

    private val routeIdProvider: RouteIdProvider = DefaultRouteIdProvider

    /** Last destination per root graph (used for tab restore behavior). */
    private val lastDestinationByRootId = mutableMapOf<Int, NavDestination>()

    /** Lookup from routeId -> logical NavDestination (only what we've seen so far). */
    private val idToDestination = mutableMapOf<Int, NavDestination>()

    /** Public stream for UI (TopBar, etc.). */
    private val _currentDestination = MutableStateFlow<NavDestination?>(null)
    val currentDestinationFlow: StateFlow<NavDestination?> = _currentDestination.asStateFlow()
    val currentDestinationSnapshot: NavDestination?
        get() = _currentDestination.value

    fun attach(controller: NavHostController) {
        navController = controller
    }

    fun detach() {
        navController = null
        lastDestinationByRootId.clear()
        idToDestination.clear()
        _currentDestination.value = null
    }

    /**
     * Called from Compose layer whenever the NavController backstack changes.
     * `destinationId == null` means "no current destination".
     */
    fun onBackstackDestinationChanged(destinationId: Int?) {
        val destination = destinationId?.let { idToDestination[it] }
        _currentDestination.value = destination
    }

    /**
     * Used when we *wissen* welche Destination aktuell ist (z. B. Startdestination).
     * Registriert sie im Cache und setzt sie als current.
     */
    fun <D : NavDestination> onBackstackDestinationChanged(destination: D?) {
        if (destination != null) {
            registerKnownDestination(destination)
        }
        _currentDestination.value = destination
    }

    /** Make sure we can later map routeId -> NavDestination. */
    private fun <D : NavDestination> registerKnownDestination(destination: D) {
        idToDestination[idOf(destination)] = destination
    }

    /** Keep current/tab state in sync after any explicit navigate. */
    private fun trackDestination(destination: NavDestination) {
        val id = idOf(destination)
        registerKnownDestination(destination)
        _currentDestination.value = destination

        rootIdForDestinationId(id)?.let { rootId ->
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
                        popUpTo(controller.graph.id) {
                            inclusive = false
                        }
                    }

                    NavOptions.Backstack.None -> Unit
                }
            }

            trackDestination(navDestination)

        } catch (e: IllegalArgumentException) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

    fun <D : NavDestination> handleSwitchTo(navDestination: D) {
        val controller = navController ?: return

        val currentDestId = controller.currentDestination?.id ?: return
        val currentRootId = rootIdForDestinationId(currentDestId)
        val targetRootId = rootIdForDestinationId(idOf(navDestination))

        if (currentRootId != null && currentRootId == targetRootId) return

        try {
            controller.navigate(navDestination) {
                launchSingleTop = true
                restoreState = true

                popUpTo(controller.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        } catch (e: Exception) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
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
            val ok = controller.popBackStack(navDestination, inclusive = inclusive)
            if (!ok) controller.popBackStack()
        }
    }

    fun navigateUp() {
        val controller = navController ?: return
        if (!controller.navigateUp()) {
            controller.popBackStack()
        }
    }

    /** Find the root graph ID that contains this destination. */
    private fun rootIdForDestinationId(targetId: Int): Int? {
        val controller = navController ?: return null
        val rootGraph = controller.graph
        val node = rootGraph.findNode(targetId) ?: return null

        val parent = node.parent ?: return node.id

        if (parent.id == rootGraph.id) {
            return node.id
        }

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
