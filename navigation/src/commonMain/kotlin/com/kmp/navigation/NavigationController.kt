package com.kmp.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Navigation implementation with a single global back stack and
 * section-aware switching.
 *
 * - [navigateTo] pushes destinations onto the back stack → navigateUp() works.
 * - [switchTo] (section) builds a full shell chain and replaces the stack.
 * - [switchTo] (destination) replaces the current section entry in-place.
 * - [navigateUp] pops the top entry, no-op if only one entry remains.
 *
 * Example stack for HomeScreenSection inside AppRootSection:
 * ```
 * switchTo(HomeScreenSection)               → Stack: [AppRootDestination, HomeScreenDestination, MovieScreenDestination]
 * navigateTo(PopularMovieDestination)       → Stack: [AppRootDestination, HomeScreenDestination, MovieScreenDestination, PopularMovieDestination]
 * navigateUp()                              → Stack: [AppRootDestination, HomeScreenDestination, MovieScreenDestination]
 * switchTo(SeriesScreenDestination)         → Stack: [AppRootDestination, HomeScreenDestination, SeriesScreenDestination]
 * switchTo(SettingsSection)                 → Stack: [AppRootDestination, SettingsScreenDestination]
 * switchTo(HomeScreenSection)               → Stack: [AppRootDestination, HomeScreenDestination, SeriesScreenDestination] ← restored
 * ```
 */
class NavigationController : Navigation {

    data class State(
        val currentDestination: NavDestination? = null,
        val currentSection: NavSection? = null,
        val backStack: List<NavDestination> = emptyList(),
        val lastEvent: NavigationEvent = NavigationEvent.Idle
    )

    private val backStack = mutableListOf<NavDestination>()
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var destinationSections: Map<KClass<out NavDestination>, NavSection> = emptyMap()
    private var sectionRoots: Map<NavSection, NavDestination> = emptyMap()
    private var parentSections: Map<NavSection, NavSection> = emptyMap()
    private val lastDestinationPerSection = mutableMapOf<NavSection, NavDestination>()
    private var lastEvent: NavigationEvent = NavigationEvent.Idle

    internal fun configureSections(
        destinationToSection: Map<KClass<out NavDestination>, NavSection>,
        sectionRoots: Map<NavSection, NavDestination>,
        parentSections: Map<NavSection, NavSection>
    ) {
        this.destinationSections = destinationToSection
        this.sectionRoots = sectionRoots
        this.parentSections = parentSections
    }

    private fun sectionOf(destination: NavDestination): NavSection? =
        destinationSections[destination::class]

    private fun updateState() {
        val current = backStack.lastOrNull()
        _state.value = State(
            currentDestination = current,
            currentSection = current?.let { sectionOf(it) },
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

        when (val backstack = navOptions.backstack) {
            is NavOptions.Backstack.None -> Unit
            is NavOptions.Backstack.Clear -> backStack.clear()
            is NavOptions.Backstack.PopTo -> {
                popToTypeInStack(backStack, backstack.navDestination::class, backstack.inclusive)
            }
        }

        if (navOptions.restoreState) {
            val idx = backStack.indexOfLast { it::class == navDestination::class }
            if (idx >= 0) {
                backStack.subList(idx + 1, backStack.size).clear()
                val restored = backStack.last()
                sectionOf(restored)?.let { lastDestinationPerSection[it] = restored }
                updateState()
                return
            }
        }

        if (navOptions.singleTop && backStack.lastOrNull() == navDestination) {
            updateState()
            return
        }

        backStack += navDestination
        sectionOf(navDestination)?.let { lastDestinationPerSection[it] = navDestination }
        updateState()
    }

    override fun switchTo(section: NavSection) {
        lastEvent = NavigationEvent.SwitchTo

        val shellChain = buildShellChain(section)
        if (shellChain.isEmpty()) { updateState(); return }

        backStack.clear()
        backStack.addAll(shellChain)

        shellChain.forEach { destination ->
            sectionOf(destination)?.let { lastDestinationPerSection[it] = destination }
        }

        updateState()
    }

    override fun <D : NavDestination> switchTo(destination: D) {
        lastEvent = NavigationEvent.SwitchTo

        val section = sectionOf(destination)
        val lastIndexInSection = if (section != null) {
            backStack.indexOfLast { sectionOf(it) == section }
        } else -1

        if (lastIndexInSection >= 0) {
            backStack.subList(lastIndexInSection, backStack.size).clear()
            backStack += destination
        } else {
            backStack.clear()
            backStack += destination
        }

        section?.let { lastDestinationPerSection[it] = destination }
        updateState()
    }

    override fun navigateUp() {
        lastEvent = NavigationEvent.NavigateUp
        if (backStack.size <= 1) { updateState(); return }
        backStack.removeLast()
        updateState()
    }

    override fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
        lastEvent = NavigationEvent.PopBackTo
        if (navDestination == null) { navigateUp(); return }
        popToTypeInStack(backStack, navDestination::class, inclusive)
        updateState()
    }

    /**
     * Builds a full destination chain from the top-level section root
     * down to the last visited destination of [targetSection].
     *
     * Example: switchTo(HomeScreenSection) where HomeScreenSection is inside AppRootSection:
     * → [AppRootDestination, HomeScreenDestination, MovieScreenDestination]
     */
    private fun buildShellChain(targetSection: NavSection): List<NavDestination> {
        val sectionChain = mutableListOf<NavSection>()
        var current: NavSection? = targetSection
        while (current != null) {
            sectionChain.add(0, current)
            current = parentSections[current]
        }

        return sectionChain.mapNotNull { section ->
            if (section == targetSection) {
                lastDestinationPerSection[section] ?: sectionRoots[section]
            } else {
                sectionRoots[section]
            }
        }
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