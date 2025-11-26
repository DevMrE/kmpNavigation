package com.devmre.kmpnavigation.compose_interface

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import co.touchlab.kermit.Logger
import com.devmre.kmpnavigation.NavDestination
import com.devmre.kmpnavigation.NavOptions
import com.devmre.kmpnavigation.RouteIdProvider
import kotlin.reflect.KClass

/**
 * Central, internal navigation logic used by:
 *  - ViewModel-driven navigation (via the Navigation implementation)
 *  - Modifier extensions (Modifier.navigateTo, switchTab, ...)
 *
 * Keeps:
 *  - the NavHostController
 *  - the last destination per root graph (to restore tabs)
 *  - a RouteIdProvider strategy
 */
internal class HandleNavigation(
    private val routeIdProvider: RouteIdProvider
) {

    var navController: NavHostController? = null
        private set

    /**
     * Remembers, per root graph (tab), the most recently visited destination.
     * Key: root navigation graph ID
     * Value: last NavDestination under that root
     */
    private val lastDestinationByRootId = mutableMapOf<Int, NavDestination>()

    fun attach(controller: NavHostController) {
        navController = controller
    }

    fun detach() {
        navController = null
        lastDestinationByRootId.clear()
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
                        // Clear the entire back stack (up to the graph root)
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

        } catch (e: IllegalArgumentException) {
            e.message?.let {
                Logger.e(tag = "Navigation", messageString = it, throwable = e)
            }
        }
    }

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
        navController?.navigateUp()
    }

    /**
     * Returns the ID of the root graph that contains the destination with [targetId].
     * Used for tab behavior (each tab has its own root).
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
