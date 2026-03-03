package com.kmp.navigation

import androidx.compose.runtime.mutableStateListOf
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

class NavigationController : Navigation {

    val backStack = mutableStateListOf<NavDestination>()

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    private var destinationSections: Map<KClass<out NavDestination>, NavSection> = emptyMap()
    private var sectionRoots: Map<NavSection, NavDestination> = emptyMap()
    private var parentSections: Map<NavSection, NavSection> = emptyMap()
    private var sectionIndices: Map<NavSection, Int> = emptyMap()

    private val lastTabPerSection = mutableMapOf<NavSection, NavDestination>()

    private var lastEvent: NavigationEvent = NavigationEvent.Idle
    private var lastTransition: NavTransitionSpec = NavTransitions.fade

    internal fun configureSections(
        destinationToSection: Map<KClass<out NavDestination>, NavSection>,
        sectionRoots: Map<NavSection, NavDestination>,
        parentSections: Map<NavSection, NavSection>,
        sectionIndices: Map<NavSection, Int>
    ) {
        this.destinationSections = destinationToSection
        this.sectionRoots = sectionRoots
        this.parentSections = parentSections
        this.sectionIndices = sectionIndices
    }

    internal fun sectionOf(destination: NavDestination): NavSection? =
        destinationSections[destination::class]

    private fun isShellRoot(destination: NavDestination): Boolean =
        sectionRoots.values.any { it::class == destination::class }

    /**
     * Returns the filtered substack for a given section.
     * Used by NavigationContent<S> to feed into NavDisplay.
     */
    fun subStackFor(section: NavSection): List<NavDestination> {
        // Find shell root index for this section
        val shellRootIndex = backStack.indexOfFirst { dest ->
            sectionRoots[section]?.let { it::class == dest::class } == true
        }
        if (shellRootIndex < 0) return emptyList()

        // Return everything from shellRoot+1 until we hit the next sibling shell root
        val result = mutableListOf<NavDestination>()
        for (i in shellRootIndex + 1 until backStack.size) {
            val dest = backStack[i]
            // Stop if we hit a shell root of a different section at the same level
            if (isShellRoot(dest) && sectionOf(dest) != section) break
            if (sectionBelongsToScope(sectionOf(dest), section)) {
                result.add(dest)
            }
        }
        return result
    }

    private fun sectionBelongsToScope(candidate: NavSection?, scope: NavSection): Boolean {
        if (candidate == null) return false
        if (candidate == scope) return true
        val parent = parentSections[candidate] ?: return false
        return sectionBelongsToScope(parent, scope)
    }

    internal fun setInitialTab(section: NavSection, destination: NavDestination) {
        if (!isShellRoot(destination)) {
            lastTabPerSection[section] = destination
        }
    }

    private fun updateState() {
        val current = backStack.lastOrNull()
        _state.value = NavigationState(
            backStack = backStack.toList(),
            currentDestination = current,
            currentSection = current?.let { sectionOf(it) },
            lastEvent = lastEvent,
            lastTransition = lastTransition
        )
    }

    override fun <D : NavDestination> navigateTo(
        destination: D,
        options: NavOptions.() -> Unit
    ) {
        val navOptions = NavOptions().apply(options)
        if (navOptions.singleTop && backStack.lastOrNull()?.let { it::class == destination::class } == true) return

        lastEvent = NavigationEvent.NavigateTo
        lastTransition = navOptions.transition ?: NavTransitions.fade

        backStack.add(destination)
        if (!isShellRoot(destination)) {
            sectionOf(destination)?.let { lastTabPerSection[it] = destination }
        }
        updateState()
    }

    override fun switchTo(section: NavSection, transition: NavTransitionSpec?) {
        val shellChain = buildShellChain(section)
        if (shellChain.isEmpty()) {
            Logger.w("NavigationController") { "switchTo($section): empty shell chain." }
            return
        }

        lastEvent = NavigationEvent.SwitchTo
        lastTransition = transition ?: defaultSwitchTransition(
            fromSection = backStack.lastOrNull()?.let { sectionOf(it) },
            toSection = section
        )

        backStack.clear()
        backStack.addAll(shellChain)

        shellChain.forEach { dest ->
            if (!isShellRoot(dest)) {
                sectionOf(dest)?.let { lastTabPerSection[it] = dest }
            }
        }
        updateState()
    }

    override fun <D : NavDestination> switchTo(destination: D, transition: NavTransitionSpec?) {
        val section = sectionOf(destination)

        lastEvent = NavigationEvent.SwitchTo
        lastTransition = transition ?: NavTransitions.fade

        val shellRootIndex = if (section != null) {
            backStack.indexOfFirst { dest ->
                sectionRoots[section]?.let { it::class == dest::class } == true
            }
        } else -1

        if (shellRootIndex >= 0) {
            val removeFrom = shellRootIndex + 1
            repeat(backStack.size - removeFrom) {
                if (backStack.size > removeFrom) backStack.removeAt(removeFrom)
            }
            backStack.add(destination)
        } else {
            backStack.clear()
            backStack.add(destination)
        }

        section?.let { lastTabPerSection[it] = destination }
        updateState()
    }

    override fun navigateUp() {
        if (backStack.size <= 1) return
        lastEvent = NavigationEvent.NavigateUp
        lastTransition = NavTransitions.fade
        backStack.removeLastOrNull()
        updateState()
    }

    override fun <D : NavDestination> popBackTo(destination: D, inclusive: Boolean) {
        val idx = backStack.indexOfLast { it::class == destination::class }
        if (idx < 0) {
            Logger.w("NavigationController") { "popBackTo: ${destination::class.simpleName} not found." }
            return
        }
        lastEvent = NavigationEvent.PopBackTo
        lastTransition = NavTransitions.fade
        val removeFrom = if (inclusive) idx else idx + 1
        if (removeFrom >= backStack.size) return
        repeat(backStack.size - removeFrom) {
            if (backStack.size > removeFrom) backStack.removeAt(removeFrom)
        }
        updateState()
    }

    override fun popBackTo(section: NavSection, inclusive: Boolean) {
        val root = sectionRoots[section]
        if (root == null) {
            Logger.w("NavigationController") { "popBackTo(section): No root for $section." }
            return
        }
        val idx = backStack.indexOfLast { it::class == root::class }
        if (idx < 0) {
            Logger.w("NavigationController") { "popBackTo(section): root not in stack." }
            return
        }
        lastEvent = NavigationEvent.PopBackTo
        lastTransition = NavTransitions.fade
        val removeFrom = if (inclusive) idx else idx + 1
        if (removeFrom >= backStack.size) return
        repeat(backStack.size - removeFrom) {
            if (backStack.size > removeFrom) backStack.removeAt(removeFrom)
        }
        updateState()
    }

    override fun <D : NavDestination> clearStackAndNavigate(destination: D) {
        lastEvent = NavigationEvent.ClearStack
        lastTransition = NavTransitions.fade
        backStack.clear()
        backStack.add(destination)
        if (!isShellRoot(destination)) {
            sectionOf(destination)?.let { lastTabPerSection[it] = destination }
        }
        updateState()
    }

    private fun buildShellChain(targetSection: NavSection): List<NavDestination> {
        val chain = mutableListOf<NavSection>()
        var current: NavSection? = targetSection
        while (current != null) {
            chain.add(0, current)
            current = parentSections[current]
        }
        return chain.mapNotNull { section ->
            if (section == targetSection) {
                val lastTab = lastTabPerSection[section]
                if (lastTab != null && !isShellRoot(lastTab)) lastTab
                else sectionRoots[section]
            } else {
                sectionRoots[section]
            }
        }
    }

    private fun defaultSwitchTransition(
        fromSection: NavSection?,
        toSection: NavSection
    ): NavTransitionSpec {
        val fromIndex = fromSection?.let { sectionIndices[it] } ?: return NavTransitions.fade
        val toIndex = sectionIndices[toSection] ?: return NavTransitions.fade
        return if (toIndex > fromIndex) NavTransitions.slideInFromRight else NavTransitions.slideInFromLeft
    }
}