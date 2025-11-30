package com.kmp.navigation.compose

import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.Navigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.emptyMap
import kotlin.collections.mutableMapOf
import kotlin.reflect.KClass

internal typealias TabKey = KClass<out NavDestination>

/**
 * Navigation handler
 */
object NavigationImpl : Navigation {

    data class State(
        val currentDestination: NavDestination? = null,
        val currentTab: TabKey? = null,
        val backStacks: Map<TabKey, List<NavDestination>> = emptyMap()
    )

    private val backStacks: MutableMap<TabKey, MutableList<NavDestination>> = mutableMapOf()
    private var currentTab: TabKey? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private fun updateState() {
        val tab = currentTab
        val current = tab?.let { backStacks[it]?.lastOrNull() }
        _state.value = State(
            currentDestination = current,
            currentTab = tab,
            backStacks = backStacks.mapValues { it.value.toList() }
        )
    }

    override fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val navOptions = NavOptions().apply(options)

        // if there is no tab, the first call will be the initial tab.
        val tab = currentTab ?: navDestination::class.also { currentTab = it }
        val stack = backStacks.getOrPut(tab) { mutableListOf() }

        // 1) BackstackOptions
        when (val backstack = navOptions.backstack) {
            NavOptions.Backstack.None -> Unit
            NavOptions.Backstack.Clear -> {
                stack.clear()
            }

            is NavOptions.Backstack.PopTo -> {
                popToInStack(stack, backstack.navDestination::class, backstack.inclusive)
            }
        }

        // 2) restoreState: restore if navDestination is already in stack
        if (navOptions.restoreState) {
            val idx = stack.indexOfLast { it::class == navDestination::class }
            if (idx >= 0) {
                stack.subList(idx + 1, stack.size).clear()
                updateState()
                return
            }
        }

        // 3) singleTop
        val currentTop = stack.lastOrNull()
        if (navOptions.singleTop && currentTop == navDestination) {
            updateState()
            return
        }

        // 4) Standard, just push
        stack += navDestination
        updateState()
    }

    override fun <D : NavDestination> switchTab(navDestination: D) {
        val tabKey: TabKey = navDestination::class

        // Für a new tab add a new root
        val stack = backStacks.getOrPut(tabKey) { mutableListOf(navDestination) }

        if (stack.isEmpty()) {
            stack += navDestination
        }

        currentTab = tabKey
        updateState()
    }

    override fun navigateUp() {
        val tab = currentTab ?: return
        val stack = backStacks[tab] ?: return

        // Don't remove the root—otherwise, the tab will be left without a screen.
        if (stack.size <= 1) return

        stack.removeLast()
        updateState()
    }

    override fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        val tab = currentTab ?: return
        val stack = backStacks[tab] ?: return

        if (navDestination == null) {
            // one step back
            if (stack.size <= 1) return
            stack.removeLast()
            updateState()
            return
        }

        val targetKey: TabKey = navDestination::class
        popToInStack(stack, targetKey, inclusive)
        updateState()
    }

    private fun popToInStack(
        stack: MutableList<NavDestination>,
        targetKey: TabKey,
        inclusive: Boolean
    ) {
        val idx = stack.indexOfLast { it::class == targetKey }
        if (idx < 0) return

        val removeFrom = if (inclusive) idx else idx + 1
        if (removeFrom >= stack.size) return

        stack.subList(removeFrom, stack.size).clear()
    }
}
