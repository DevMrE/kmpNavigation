package com.kmp.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Navigation implementation with a single global back stack and
 * section-aware switching.
 *
 * * [navigateTo] pushes destinations on a global stack.
 * * [switchTo] uses "last screen per section" + configured root to decide where to go.
 * * [navigateUp] always goes to the previously shown screen, even if it
 *   belongs to a different section.
 *
 * Example behavior:
 *
 * 1. HomeSection - MovieScreenDestination
 * 2. navigateTo(SeriesScreenDestination)    -> HomeSection - Series
 * 3. switchTo(AuthSection)                  -> AuthSection - Login
 * 4. navigateUp()                           -> HomeSection - Series
 */
class NavigationController : Navigation {

    /**
     * Snapshot of the current navigation state.
     *
     * You usually do not consume this directly. Compose helpers
     * (rememberNavDestination, rememberNavSection, NavigationContent)
     * wrap it for you.
     */
    data class State(
        val currentDestination: NavDestination? = null,
        val currentSection: NavSection? = null,
        val backStack: List<NavDestination> = emptyList(),
        val lastEvent: NavigationEvent = NavigationEvent.Idle
    )

    private val backStack = mutableListOf<NavDestination>()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // destination type -> section instance
    private var destinationSections: Map<KClass<out NavDestination>, NavSection> =
        emptyMap()

    // section instance -> configured root destination instance
    private var sectionRoots: Map<NavSection, NavDestination> =
        emptyMap()

    // section instance -> last visited destination of that section
    private val lastDestinationPerSection =
        mutableMapOf<NavSection, NavDestination>()

    // last operation that changed the back stack
    private var lastEvent: NavigationEvent = NavigationEvent.Idle

    /**
     * Configure section information.
     *
     * Called by the navigation graph builder after the DSL has been evaluated.
     */
    internal fun configureSections(
        destinationToSection: Map<KClass<out NavDestination>, NavSection>,
        sectionRoots: Map<NavSection, NavDestination>
    ) {
        this.destinationSections = destinationToSection
        this.sectionRoots = sectionRoots
        // we do not touch the current back stack here – this is metadata only
    }

    private fun sectionOf(destination: NavDestination): NavSection? =
        destinationSections[destination::class]

    private fun updateState() {
        val current = backStack.lastOrNull()
        val currentSection = current?.let { sectionOf(it) }
        _state.value = State(
            currentDestination = current,
            currentSection = currentSection,
            backStack = backStack.toList(),
            lastEvent = lastEvent
        )
    }

    override fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        lastEvent = NavigationEvent.NavigateTo

        val navOptions = NavOptions().apply(options)

        // Handle back stack options on the global stack
        when (val backstack = navOptions.backstack) {
            is NavOptions.Backstack.None -> Unit
            is NavOptions.Backstack.Clear -> backStack.clear()
            is NavOptions.Backstack.PopTo -> {
                popToTypeInStack(backStack, backstack.navDestination::class, backstack.inclusive)
            }
        }

        // restoreState: jump back to the last destination of the same type instead of pushing
        if (navOptions.restoreState) {
            val idx = backStack.indexOfLast { it::class == navDestination::class }
            if (idx >= 0) {
                backStack.subList(idx + 1, backStack.size).clear()
                val restored = backStack.last()
                sectionOf(restored)?.let { section ->
                    lastDestinationPerSection[section] = restored
                }
                updateState()
                return
            }
        }

        val current = backStack.lastOrNull()
        if (navOptions.singleTop && current == navDestination) {
            updateState()
            return
        }

        backStack += navDestination

        // update "last screen per section"
        sectionOf(navDestination)?.let { section ->
            lastDestinationPerSection[section] = navDestination
        }

        updateState()
    }

    override fun switchTo(section: NavSection) {
        lastEvent = NavigationEvent.SwitchTo

        // 1) Look up last visited destination of this section
        val target = lastDestinationPerSection[section]
        // 2) fall back to configured root
            ?: sectionRoots[section]
            // 3) if we really have nothing, do nothing
            ?: run {
                updateState()
                return
            }

        val current = backStack.lastOrNull()
        if (current == target) {
            // Already on that screen – no-op
            updateState()
            return
        }

        // 4) Push target as a new history entry
        backStack += target

        // update last for section
        sectionOf(target)?.let { s ->
            lastDestinationPerSection[s] = target
        }

        updateState()
    }

    override fun navigateUp() {
        lastEvent = NavigationEvent.NavigateUp

        if (backStack.size <= 1) {
            updateState()
            return
        }
        backStack.removeLast()
        updateState()
    }

    override fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        lastEvent = NavigationEvent.PopBackTo

        if (navDestination == null) {
            navigateUp()
            return
        }

        popToTypeInStack(backStack, navDestination::class, inclusive)
        updateState()
    }

    private fun popToTypeInStack(
        stack: MutableList<NavDestination>,
        type: KClass<out NavDestination>,
        inclusive: Boolean
    ) {
        val idx = stack.indexOfLast { it::class == type }
        if (idx < 0) return

        val removeFrom = if (inclusive) idx else idx + 1
        if (removeFrom >= stack.size) return

        stack.subList(removeFrom, stack.size).clear()
    }
}
