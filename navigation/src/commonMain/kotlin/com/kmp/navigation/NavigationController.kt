package com.kmp.navigation

import androidx.compose.runtime.mutableStateListOf
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Core navigation implementation.
 *
 * BackStack rules:
 * - `tabs` destinations → never added to BackStack, last active per group is remembered
 * - `screen` / `content` destinations → added to BackStack normally
 *
 * Tab state restoration:
 * - Switching AppRoot tabs restores the last active destination of that tab
 * - Example: HomeTabs had SeriesContent active → switch to Settings → switch back → SeriesContent restored
 */
class NavigationController : Navigation {

    internal val backStack = mutableStateListOf<NavDestination>()

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    // Last active destination per group – for tab state restoration
    // Key: group KClass, Value: last active destination in that group
    private val lastActivePerGroup = mutableMapOf<KClass<out NavGroup>, NavDestination>()

    // Last active destination per tabs-destination that is itself in a parent group
    // This handles nested tabs correctly
    private var lastEvent: NavigationEvent = NavigationEvent.Idle

    private fun updateState() {
        _state.value = NavigationState(
            backStack = backStack.toList(),
            currentDestination = backStack.lastOrNull(),
            lastEvent = lastEvent
        )
        Logger.i("NavigationController") {
            "backStack: $backStack"
        }
    }

    /**
     * Initialize the BackStack with the start destination.
     * Handles tab groups correctly by resolving the start destination chain.
     */
    internal fun initialize(startDestination: NavDestination) {
        if (backStack.isNotEmpty()) return

        lastEvent = NavigationEvent.Idle

        // Build the initial stack by resolving the start destination
        val initialStack = resolveInitialStack(startDestination)
        backStack.addAll(initialStack)

        updateState()
    }

    /**
     * Resolves the initial stack for a start destination.
     *
     * If startDestination is in a tabs group that is itself referenced by
     * a parent tabs group, we need to build the full chain.
     */
    private fun resolveInitialStack(startDestination: NavDestination): List<NavDestination> {
        val result = mutableListOf<NavDestination>()

        // Find which group the startDestination belongs to
        val groupClass = NavigationGraph.groupOf(startDestination)

        if (groupClass != null) {
            // startDestination is a tab – find parent tabs that reference this group
            // Build stack: [parentTabDest, ..., startDestination]
            val parentDest = findParentTabDestinationFor(groupClass)
            if (parentDest != null) {
                result.addAll(resolveInitialStack(parentDest))
            }
            // Remember this as the active destination for the group
            lastActivePerGroup[groupClass] = startDestination
        }

        result.add(startDestination)
        return result
    }

    /**
     * Finds the destination in a parent group that "contains" this group.
     * Used to build the initial stack correctly.
     *
     * Example: HomeTabs contains MovieContentDestination.
     * AppRoot contains HomeContentDestination.
     * HomeContent renders NavigationContent<HomeTabs>.
     * → parent of HomeTabs is AppRoot, via HomeContentDestination.
     *
     * We find this by checking which destination's screen contains NavigationContent<G>.
     * Since we can't inspect composables, we rely on explicit nesting hints.
     * For now, we look through all groups to find if any group's destinations
     * are "parent" destinations that render sub-groups.
     *
     * This is a best-effort lookup – returns null if no parent found.
     */
    private fun findParentTabDestinationFor(groupClass: KClass<out NavGroup>): NavDestination? {
        // We rely on the registered group nesting info set during registration
        return groupParents[groupClass]
    }

    // Group parent destination lookup: group KClass → parent destination that renders it
    // Set during registerNavigation via groupNesting
    private val groupParents = mutableMapOf<KClass<out NavGroup>, NavDestination>()

    internal fun setGroupParents(parents: Map<KClass<out NavGroup>, NavDestination>) {
        groupParents.putAll(parents)
    }

    override fun navigateTo(destination: NavDestination) {
        val groupClass = NavigationGraph.groupOf(destination)

        if (groupClass != null) {
            // Destination is a tab → switch tab, no BackStack entry
            switchTab(destination, groupClass)
        } else {
            // screen or content → add to BackStack
            pushToBackStack(destination)
        }
    }

    /**
     * Switches to a tab destination within its group.
     * Does NOT add to BackStack.
     * Remembers the last active destination per group.
     */
    private fun switchTab(destination: NavDestination, groupClass: KClass<out NavGroup>) {
        lastEvent = NavigationEvent.SwitchTab

        // Find the parent destination for this group in the current stack
        val parentDest = groupParents[groupClass]

        if (parentDest != null) {
            // Find where the parent destination is in the backStack
            val parentIndex = backStack.indexOfLast { it::class == parentDest::class }

            if (parentIndex >= 0) {
                // Remove everything after the parent destination
                // (screen destinations that were pushed on top)
                val removeFrom = parentIndex + 1
                repeat(backStack.size - removeFrom) {
                    if (backStack.size > removeFrom) backStack.removeAt(removeFrom)
                }
            }
        }

        // Remember the active destination for this group
        lastActivePerGroup[groupClass] = destination

        updateState()
    }

    /**
     * Pushes a screen or content destination onto the BackStack.
     */
    private fun pushToBackStack(destination: NavDestination) {
        // Avoid duplicate on top
        if (backStack.lastOrNull()?.let { it::class == destination::class } == true) {
            Logger.d("NavigationController") {
                "navigateTo: ${destination::class.simpleName} already on top – skipping."
            }
            return
        }

        lastEvent = NavigationEvent.NavigateTo
        backStack.add(destination)
        updateState()
    }

    override fun navigateUp() {
        if (backStack.size <= 1) {
            Logger.d("NavigationController") { "navigateUp: nothing to pop." }
            return
        }

        lastEvent = NavigationEvent.NavigateUp
        backStack.removeLastOrNull()
        updateState()
    }

    override fun popBackTo(destination: NavDestination, inclusive: Boolean) {
        val idx = backStack.indexOfLast { it::class == destination::class }
        if (idx < 0) {
            Logger.w("NavigationController") {
                "popBackTo: ${destination::class.simpleName} not found in backStack."
            }
            return
        }

        lastEvent = NavigationEvent.PopBackTo
        val removeFrom = if (inclusive) idx else idx + 1
        if (removeFrom >= backStack.size) return

        repeat(backStack.size - removeFrom) {
            if (backStack.size > removeFrom) backStack.removeAt(removeFrom)
        }
        updateState()
    }

    override fun clearStackAndNavigateTo(destination: NavDestination) {
        lastEvent = NavigationEvent.ClearStack
        backStack.clear()
        backStack.add(destination)

        // Also clear tab state
        lastActivePerGroup.clear()
        val groupClass = NavigationGraph.groupOf(destination)
        if (groupClass != null) {
            lastActivePerGroup[groupClass] = destination
        }

        updateState()
    }

    /**
     * Returns the currently active destination for a tabs group.
     * Falls back to the group's startDestination if never visited.
     */
    fun activeDestinationFor(groupClass: KClass<out NavGroup>): NavDestination? {
        return lastActivePerGroup[groupClass]
            ?: NavigationGraph.startDestinationFor(groupClass)
    }

    /**
     * Returns the current destination in the BackStack that belongs to
     * the given group. Used by rememberCurrentTabInGroup.
     */
    internal fun currentDestinationInGroup(groupClass: KClass<out NavGroup>): NavDestination? {
        return lastActivePerGroup[groupClass]
            ?: NavigationGraph.startDestinationFor(groupClass)
    }
}