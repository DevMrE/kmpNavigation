package com.kmp.navigation

import androidx.compose.runtime.mutableStateListOf
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Core navigation implementation backed by a [SnapshotStateList].
 *
 * This is the single source of truth for navigation state.
 * Inject via Koin or access via [GlobalNavigation].
 */
class NavigationController : Navigation {

    // The back stack – a SnapshotStateList so Compose observes it directly
    val backStack = mutableStateListOf<NavDestination>()

    private val _state = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = _state.asStateFlow()

    // Section metadata – set by NavigationGraph after DSL evaluation
    private var destinationSections: Map<KClass<out NavDestination>, NavSection> = emptyMap()
    private var sectionRoots: Map<NavSection, NavDestination> = emptyMap()
    private var parentSections: Map<NavSection, NavSection> = emptyMap()
    private var sectionIndices: Map<NavSection, Int> = emptyMap()

    // Last visited destination per section – for state restoration
    private val lastDestinationPerSection = mutableMapOf<NavSection, NavDestination>()

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

    internal fun sectionIndexOf(destination: NavDestination): Int? {
        val section = sectionOf(destination) ?: return null
        return sectionIndices[section]
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

        if (navOptions.singleTop && backStack.lastOrNull()?.let { it::class == destination::class } == true) {
            return
        }

        lastEvent = NavigationEvent.NavigateTo
        lastTransition = navOptions.transition ?: NavTransitions.fade

        backStack.add(destination)
        sectionOf(destination)?.let { lastDestinationPerSection[it] = destination }
        updateState()
    }

    override fun switchTo(section: NavSection, transition: NavTransitionSpec?) {
        val shellChain = buildShellChain(section)
        if (shellChain.isEmpty()) {
            Logger.w("NavigationController") { "switchTo($section): Could not build shell chain. No sectionRoots found." }
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
            sectionOf(dest)?.let { lastDestinationPerSection[it] = dest }
        }
        updateState()
    }

    override fun <D : NavDestination> switchTo(destination: D, transition: NavTransitionSpec?) {
        val section = sectionOf(destination)

        lastEvent = NavigationEvent.SwitchTo
        lastTransition = transition ?: NavTransitions.fade

        val lastIndexInSection = if (section != null) {
            backStack.indexOfLast { sectionOf(it) == section }
        } else -1

        if (lastIndexInSection >= 0) {
            // Replace everything from that index onwards with the new destination
            val removeFrom = lastIndexInSection
            repeat(backStack.size - removeFrom) {
                if (backStack.size > removeFrom) backStack.removeAt(removeFrom)
            }
            backStack.add(destination)
        } else {
            backStack.clear()
            backStack.add(destination)
        }

        section?.let { lastDestinationPerSection[it] = destination }
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
            Logger.w("NavigationController") { "popBackTo: ${destination::class.simpleName} not found in backStack." }
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
            Logger.w("NavigationController") { "popBackTo(section): No root found for $section." }
            return
        }

        val idx = backStack.indexOfLast { it::class == root::class }
        if (idx < 0) {
            Logger.w("NavigationController") { "popBackTo(section): Root ${root::class.simpleName} not found in backStack." }
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

        sectionOf(destination)?.let { lastDestinationPerSection[it] = destination }
        updateState()
    }

    /**
     * Builds the full shell chain from the outermost ancestor section
     * down to [targetSection].
     *
     * Example: switchTo(HomeSection) where HomeSection ⊂ AppRootSection:
     * → [AppRootDestination, HomeDestination, MovieDestination]
     */
    private fun buildShellChain(targetSection: NavSection): List<NavDestination> {
        // Collect ancestry: [AppRootSection, HomeSection]
        val chain = mutableListOf<NavSection>()
        var current: NavSection? = targetSection
        while (current != null) {
            chain.add(0, current)
            current = parentSections[current]
        }

        return chain.mapNotNull { section ->
            if (section == targetSection) {
                lastDestinationPerSection[section] ?: sectionRoots[section]
            } else {
                sectionRoots[section]
            }
        }
    }

    /**
     * Determines the default slide direction based on section indices.
     */
    private fun defaultSwitchTransition(
        fromSection: NavSection?,
        toSection: NavSection
    ): NavTransitionSpec {
        val fromIndex = fromSection?.let { sectionIndices[it] } ?: return NavTransitions.fade
        val toIndex = sectionIndices[toSection] ?: return NavTransitions.fade
        return if (toIndex > fromIndex) NavTransitions.slideInFromRight else NavTransitions.slideInFromLeft
    }
}