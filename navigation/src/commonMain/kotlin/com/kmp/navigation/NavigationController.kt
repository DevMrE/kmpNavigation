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
    private var sectionDefaults: Map<NavSection, NavDestination> = emptyMap()
    internal var parentSections: Map<NavSection, NavSection> = emptyMap()
        private set
    private var sectionIndices: Map<NavSection, Int> = emptyMap()

    private val lastTabPerSection = mutableMapOf<NavSection, NavDestination>()

    private var lastEvent: NavigationEvent = NavigationEvent.Idle
    private var lastTransition: NavTransitionSpec = NavTransitions.fade

    internal fun configureSections(
        destinationToSection: Map<KClass<out NavDestination>, NavSection>,
        sectionRoots: Map<NavSection, NavDestination>,
        sectionDefaults: Map<NavSection, NavDestination>,
        parentSections: Map<NavSection, NavSection>,
        sectionIndices: Map<NavSection, Int>
    ) {
        this.destinationSections = destinationToSection
        this.sectionRoots = sectionRoots
        this.sectionDefaults = sectionDefaults
        this.parentSections = parentSections
        this.sectionIndices = sectionIndices
    }

    internal fun sectionOf(destination: NavDestination): NavSection? =
        destinationSections[destination::class]

    internal fun parentSectionOf(section: NavSection): NavSection? =
        parentSections[section]

    private fun isShellRoot(destination: NavDestination): Boolean =
        sectionRoots.values.any { it::class == destination::class }

    /**
     * Builds the initial back stack.
     *
     * startDestination = AppRootDestination:
     * → AppRootDestination
     * AppRootSection SubStack → [] → triggers default → [HomeScreenDestination, MovieScreenDestination]
     */
    internal fun buildInitialStack(startDestination: NavDestination) {
        backStack.clear()

        val startSection = sectionOf(startDestination) ?: run {
            backStack.add(startDestination)
            updateState()
            return
        }

        // Build full ancestor chain
        val sectionChain = mutableListOf<NavSection>()
        var current: NavSection? = startSection
        while (current != null) {
            sectionChain.add(0, current)
            current = parentSections[current]
        }

        // Push shell roots for all ancestors
        sectionChain.dropLast(1).forEach { ancestorSection ->
            sectionRoots[ancestorSection]?.let { backStack.add(it) }
        }

        // Push startDestination
        backStack.add(startDestination)

        // If startDestination is a shell root, push the default of that section
        if (isShellRoot(startDestination)) {
            pushSectionDefault(startSection)
        } else {
            lastTabPerSection[startSection] = startDestination
        }

        Logger.i("NavigationController") {
            "buildInitialStack → backStack: $backStack"
        }

        updateState()
    }

    /**
     * Pushes the default destination of a section onto the stack.
     * If no default is set, pushes the shell root of the first child section.
     */
    private fun pushSectionDefault(section: NavSection) {
        val default = sectionDefaults[section] ?: return
        backStack.add(default)

        val defaultSection = sectionOf(default)
        if (defaultSection != null && defaultSection != section) {
            // Default belongs to a child section – push its default too if needed
            if (isShellRoot(default)) {
                pushSectionDefault(defaultSection)
            } else {
                lastTabPerSection[defaultSection] = default
            }
        } else if (defaultSection == section && isShellRoot(default)) {
            pushSectionDefault(section)
        } else if (defaultSection != null) {
            lastTabPerSection[defaultSection] = default
        }
    }

    /**
     * Returns the substack for rendering in NavigationContent<S>.
     *
     * Example:
     * Stack: [AppRootDestination, HomeScreenDestination, MovieScreenDestination]
     * subStackFor(AppRootSection)    → HomeScreenDestination
     * subStackFor(HomeScreenSection) → MovieScreenDestination
     */
    internal fun subStackFor(section: NavSection): List<NavDestination> {
        val root = sectionRoots[section] ?: return emptyList()
        val sectionParent = parentSections[section]

        val shellRootIndex = backStack.indexOfFirst { it::class == root::class }
        if (shellRootIndex < 0) {
            Logger.w("NavigationController") {
                "subStackFor(${section::class.simpleName}): shell root not found in backStack."
            }
            return emptyList()
        }

        val result = mutableListOf<NavDestination>()

        for (i in shellRootIndex + 1 until backStack.size) {
            val dest = backStack[i]
            val destSection = sectionOf(dest) ?: continue
            val destParent = parentSections[destSection]

            // Stop if sibling section at same level
            if (destParent == sectionParent && destSection != section) break

            // Only direct children of this section
            if (destSection == section) {
                result.add(dest)
            } else {
                // Deeper nested – stop, their own NavigationContent handles it
                break
            }
        }

        Logger.d("NavigationController") {
            "subStackFor(${section::class.simpleName}) → $result"
        }

        return result
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
        if (navOptions.singleTop && backStack.lastOrNull()
                ?.let { it::class == destination::class } == true
        ) return

        lastEvent = NavigationEvent.NavigateTo
        lastTransition = navOptions.transition ?: NavTransitions.fade

        backStack.add(destination)
        if (!isShellRoot(destination)) {
            sectionOf(destination)?.let { lastTabPerSection[it] = destination }
        }

        Logger.i("NavigationController") {
            "navigateTo(${destination::class.simpleName}) → backStack: $backStack"
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

        Logger.i("NavigationController") {
            "switchTo(section=${section::class.simpleName}) → backStack: $backStack"
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

        Logger.i("NavigationController") {
            "switchTo(destination=${destination::class.simpleName}) → backStack: $backStack"
        }

        updateState()
    }

    override fun navigateUp() {
        if (backStack.size <= 1) return
        lastEvent = NavigationEvent.NavigateUp
        lastTransition = NavTransitions.fade
        backStack.removeLastOrNull()

        Logger.i("NavigationController") {
            "navigateUp() → backStack: $backStack"
        }

        updateState()
    }

    override fun <D : NavDestination> popBackTo(destination: D, inclusive: Boolean) {
        val idx = backStack.indexOfLast { it::class == destination::class }
        if (idx < 0) {
            Logger.w("NavigationController") {
                "popBackTo: ${destination::class.simpleName} not found."
            }
            return
        }
        lastEvent = NavigationEvent.PopBackTo
        lastTransition = NavTransitions.fade
        val removeFrom = if (inclusive) idx else idx + 1
        if (removeFrom >= backStack.size) return
        repeat(backStack.size - removeFrom) {
            if (backStack.size > removeFrom) backStack.removeAt(removeFrom)
        }

        Logger.i("NavigationController") {
            "popBackTo(${destination::class.simpleName}, inclusive=$inclusive) → backStack: $backStack"
        }

        updateState()
    }

    override fun popBackTo(section: NavSection, inclusive: Boolean) {
        val root = sectionRoots[section]
        if (root == null) {
            Logger.w("NavigationController") {
                "popBackTo(section): No root for $section."
            }
            return
        }
        val idx = backStack.indexOfLast { it::class == root::class }
        if (idx < 0) {
            Logger.w("NavigationController") {
                "popBackTo(section): root not in stack."
            }
            return
        }
        lastEvent = NavigationEvent.PopBackTo
        lastTransition = NavTransitions.fade
        val removeFrom = if (inclusive) idx else idx + 1
        if (removeFrom >= backStack.size) return
        repeat(backStack.size - removeFrom) {
            if (backStack.size > removeFrom) backStack.removeAt(removeFrom)
        }

        Logger.i("NavigationController") {
            "popBackTo(section=${section::class.simpleName}, inclusive=$inclusive) → backStack: $backStack"
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

        Logger.i("NavigationController") {
            "clearStackAndNavigate(${destination::class.simpleName}) → backStack: $backStack"
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

        val result = mutableListOf<NavDestination>()

        chain.forEach { section ->
            if (section == targetSection) {
                val lastTab = lastTabPerSection[section]
                val dest = if (lastTab != null && !isShellRoot(lastTab)) {
                    lastTab
                } else {
                    sectionDefaults[section] ?: sectionRoots[section]
                }
                dest?.let { result.add(it) }
            } else {
                sectionRoots[section]?.let { result.add(it) }
            }
        }

        Logger.i("NavigationController") {
            "buildShellChain(${targetSection::class.simpleName}) → $result"
        }

        return result
    }

    private fun defaultSwitchTransition(
        fromSection: NavSection?,
        toSection: NavSection
    ): NavTransitionSpec {
        val fromIndex = fromSection?.let { sectionIndices[it] } ?: return NavTransitions.fade
        val toIndex = sectionIndices[toSection] ?: return NavTransitions.fade
        return if (toIndex > fromIndex) NavTransitions.slideInFromRight
        else NavTransitions.slideInFromLeft
    }
}