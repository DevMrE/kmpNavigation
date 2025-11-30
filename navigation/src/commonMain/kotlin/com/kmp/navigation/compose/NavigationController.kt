package com.kmp.navigation.compose

import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.Navigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass
import com.kmp.navigation.rememberNavDestination

/**
 * Default implementation of [Navigation] used by the library.
 *
 * It behaves similar to Voyager's navigator:
 *
 * * Each "tab" (or root section) owns its own back stack.
 * * [navigateTo] always pushes onto the **current** tab stack.
 * * [switchTab] changes the active tab and restores its last stack.
 * * [navigateUp] pops the last destination of the active tab (but never the tab root).
 *
 * You normally do **not** interact with [NavigationController] directly.
 * Instead you:
 *
 * ```kotlin
 * // 1) Create the instance in DI or manually
 * val navigation: Navigation = NavigationFactory.create()
 *
 * // 2) Inject it into your ViewModel
 * class MyViewModel(
 *     private val navigation: Navigation
 * ) : ViewModel() {
 *     fun onSettingsClick() {
 *         navigation.navigateTo(SettingsScreenDestination)
 *     }
 * }
 *
 * // 3) Use it from Compose via rememberNavigation()
 * @Composable
 * fun MyBottomBar() {
 *     val navigation = rememberNavigation()
 *     NavigationBarItem(
 *         selected = ...,
 *         onClick = { navigation.switchTab(HomeScreenDestination) },
 *         icon = { Icon(...) }
 *     )
 * }
 * ```
 */
object NavigationController : Navigation {

    /**
     * Snapshot of the current navigation state.
     *
     * You rarely need this directly. The compose helpers
     * [rememberNavDestination] and
     * [NavigationHost] consume it for you.
     */
    data class State(
        /** The currently visible destination (top of the active tab stack). */
        val currentDestination: NavDestination? = null,
        /** The destination type that acts as the root of the active tab stack. */
        val currentTabRoot: KClass<out NavDestination>? = null,
        /** The full back stack of the active tab, from root to current. */
        val currentBackStack: List<NavDestination> = emptyList(),
        /** All tab stacks keyed by their root destination type. */
        val tabBackStacks: Map<KClass<out NavDestination>, List<NavDestination>> = emptyMap()
    )

    private val tabStacks: MutableMap<KClass<out NavDestination>, MutableList<NavDestination>> =
        mutableMapOf()

    private var currentTabRoot: KClass<out NavDestination>? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private fun updateState() {
        val tabKey = currentTabRoot
        val stack = tabKey?.let { tabStacks[it] }.orEmpty()
        val current = stack.lastOrNull()

        _state.value = State(
            currentDestination = current,
            currentTabRoot = tabKey,
            currentBackStack = stack.toList(),
            tabBackStacks = tabStacks.mapValues { it.value.toList() }
        )
    }

    override fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val navOptions = NavOptions().apply(options)

        // Define or reuse current tab based on the first navigateTo call
        val tabKey = currentTabRoot ?: navDestination::class.also {
            currentTabRoot = it
        }
        val stack = tabStacks.getOrPut(tabKey) { mutableListOf() }

        // Apply back stack options
        when (val backstack = navOptions.backstack) {
            is NavOptions.Backstack.None -> Unit
            is NavOptions.Backstack.Clear -> stack.clear()
            is NavOptions.Backstack.PopTo -> {
                popToInStack(stack, backstack.navDestination::class, backstack.inclusive)
            }
        }

        // restoreState – jump back to last destination of the same type instead of pushing
        if (navOptions.restoreState) {
            val idx = stack.indexOfLast { it::class == navDestination::class }
            if (idx >= 0) {
                stack.subList(idx + 1, stack.size).clear()
                updateState()
                return
            }
        }

        // singleTop – avoid pushing the exact same instance again
        val currentTop = stack.lastOrNull()
        if (navOptions.singleTop && currentTop == navDestination) {
            updateState()
            return
        }

        stack += navDestination
        updateState()
    }

    override fun <D : NavDestination> switchTab(navDestination: D) {
        val tabKey = navDestination::class
        val stack = tabStacks.getOrPut(tabKey) { mutableListOf() }

        // First time we see this tab: push the given destination as its root
        if (stack.isEmpty()) {
            stack += navDestination
        }

        currentTabRoot = tabKey
        updateState()
    }

    override fun navigateUp() {
        val tabKey = currentTabRoot ?: return
        val stack = tabStacks[tabKey] ?: return

        // Never pop the last element (tab root) of the current stack
        if (stack.size <= 1) return

        stack.removeLast()
        updateState()
    }

    override fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        val tabKey = currentTabRoot ?: return
        val stack = tabStacks[tabKey] ?: return

        if (navDestination == null) {
            // Pop a single element
            if (stack.size <= 1) return
            stack.removeLast()
            updateState()
            return
        }

        popToInStack(stack, navDestination::class, inclusive)
        updateState()
    }

    private fun popToInStack(
        stack: MutableList<NavDestination>,
        target: KClass<out NavDestination>,
        inclusive: Boolean
    ) {
        val idx = stack.indexOfLast { it::class == target }
        if (idx < 0) return

        val removeFrom = if (inclusive) idx else idx + 1
        if (removeFrom >= stack.size) return

        stack.subList(removeFrom, stack.size).clear()
    }
}
