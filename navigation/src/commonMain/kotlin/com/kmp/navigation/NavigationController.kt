package com.kmp.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Multi-backstack controller with nested sections.
 *
 * - Each [NavSection] has its own backstack.
 * - Sections may be nested: parent -> child
 * - Parent keeps an "active child" pointer (like bottom nav or tab nav)
 * - Leaf destination = deepest active child, BUT only while parents are on their root destination.
 *
 * State restoration:
 * - If you use NavOptions.popTo(saveState=true), the current stack of that section is saved.
 * - If you later navigate to the SECTION ROOT with restoreState=true, the saved stack can be restored.
 */
class NavigationController : Navigation {

    data class State(
        val rootSection: NavSection? = null,
        val currentSection: NavSection? = null,        // leaf-most active section
        val currentPath: List<NavSection> = emptyList(),
        val currentDestination: NavDestination? = null, // leaf-most destination
        val backStacks: Map<NavSection, List<NavDestination>> = emptyMap(),
        val activeChild: Map<NavSection, NavSection> = emptyMap(),
        val lastEvent: NavigationEvent = NavigationEvent.Idle
    )

    private val sectionStacks = mutableMapOf<NavSection, MutableList<NavDestination>>()

    // parent -> currently active child
    private val activeChild = mutableMapOf<NavSection, NavSection>()

    // section -> last saved stack (for popTo(saveState=true) + restoreState)
    private val savedStacks = mutableMapOf<NavSection, List<NavDestination>>()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // destination type -> section
    private var destinationSections: Map<KClass<out NavDestination>, NavSection> = emptyMap()

    // destination type -> role
    private var destinationRoles: Map<KClass<out NavDestination>, ScreenRole> = emptyMap()

    // section -> root destination
    private var sectionRoots: Map<NavSection, NavDestination> = emptyMap()

    // child -> parent
    private var sectionParents: Map<NavSection, NavSection?> = emptyMap()

    // parent -> children
    private var sectionChildren: Map<NavSection, List<NavSection>> = emptyMap()

    // section -> sibling index
    private var sectionSiblingIndex: Map<NavSection, Int> = emptyMap()

    private var lastEvent: NavigationEvent = NavigationEvent.Idle

    // currently active graph root section
    private var activeRoot: NavSection? = null

    internal fun configureGraph(
        destinationToSection: Map<KClass<out NavDestination>, NavSection>,
        destinationRoles: Map<KClass<out NavDestination>, ScreenRole>,
        sectionRoots: Map<NavSection, NavDestination>,
        sectionParents: Map<NavSection, NavSection?>,
        sectionChildren: Map<NavSection, List<NavSection>>,
        sectionSiblingIndex: Map<NavSection, Int>,
    ) {
        this.destinationSections = destinationToSection
        this.destinationRoles = destinationRoles
        this.sectionRoots = sectionRoots
        this.sectionParents = sectionParents
        this.sectionChildren = sectionChildren
        this.sectionSiblingIndex = sectionSiblingIndex
        updateState()
    }

    internal fun roleOf(destination: NavDestination): ScreenRole =
        destinationRoles[destination::class] ?: ScreenRole.Normal

    private fun sectionOf(destination: NavDestination): NavSection? =
        destinationSections[destination::class]

    private fun rootOf(section: NavSection): NavDestination? = sectionRoots[section]

    private fun parentOf(section: NavSection): NavSection? = sectionParents[section]

    private fun childrenOf(section: NavSection): List<NavSection> = sectionChildren[section].orEmpty()

    private fun stackFor(section: NavSection): MutableList<NavDestination> =
        sectionStacks.getOrPut(section) { mutableListOf() }

    private fun ensureInitialized(section: NavSection) {
        val stack = stackFor(section)
        if (stack.isNotEmpty()) return

        val root = rootOf(section)
            ?: error("No root destination registered for section ${section::class.simpleName}.")
        stack += root
    }

    private fun ensureInitializedWithAncestors(section: NavSection) {
        var current: NavSection? = section
        while (current != null) {
            ensureInitialized(current)
            current = parentOf(current)
        }
    }

    private fun ensureActiveDescendantsFrom(section: NavSection?) {
        var current = section ?: return
        while (true) {
            val children = childrenOf(current)
            if (children.isEmpty()) return

            val child = activeChild[current] ?: children.first().also { activeChild[current] = it }
            ensureInitialized(child)
            current = child
        }
    }

    private fun activateSection(section: NavSection) {
        ensureInitializedWithAncestors(section)

        // Activate chain up to the graph root
        var current: NavSection = section
        var parent = parentOf(current)
        while (parent != null) {
            activeChild[parent] = current
            current = parent
            parent = parentOf(current)
        }
        activeRoot = current

        // Ensure defaults for nested children (so nested hosts can render immediately)
        ensureActiveDescendantsFrom(activeRoot)
    }

    /**
     * Dive into children only while the parent is on its root destination.
     */
    private fun computeCurrentPathAndDestination(): Pair<List<NavSection>, NavDestination?> {
        val root = activeRoot ?: return emptyList<NavSection>() to null

        val path = mutableListOf<NavSection>()
        var current = root

        while (true) {
            path += current

            val stack = sectionStacks[current].orEmpty()
            val top = stack.lastOrNull()
            val rootDest = rootOf(current)
            val child = activeChild[current]

            val canDive =
                child != null &&
                        top != null &&
                        rootDest != null &&
                        top::class == rootDest::class

            if (canDive) {
                current = child
                continue
            }

            return path to top
        }
    }

    private fun updateState() {
        val (path, destination) = computeCurrentPathAndDestination()
        val currentSection = path.lastOrNull()

        _state.value = State(
            rootSection = activeRoot,
            currentSection = currentSection,
            currentPath = path,
            currentDestination = destination,
            backStacks = sectionStacks.mapValues { it.value.toList() },
            activeChild = activeChild.toMap(),
            lastEvent = lastEvent
        )
    }

    override fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        lastEvent = NavigationEvent.NavigateTo

        val targetSection = sectionOf(navDestination)
            ?: error(
                "No NavSection registered for destination ${navDestination::class.simpleName}. " +
                        "Did you forget to declare it inside a section{ } block in registerNavigation()?"
            )

        val navOptions = NavOptions().apply(options)

        // Apply backstack options first
        when (val backstack = navOptions.backstack) {
            is NavOptions.Backstack.Clear -> {
                sectionStacks.clear()
                activeChild.clear()
                savedStacks.clear()
                activeRoot = null
            }

            is NavOptions.Backstack.PopTo -> {
                val sectionForPop = sectionOf(backstack.navDestination) ?: targetSection
                val stackForPop = sectionStacks[sectionForPop]

                if (stackForPop != null) {
                    if (backstack.saveState) {
                        savedStacks[sectionForPop] = stackForPop.toList()
                    }

                    popToTypeInStack(
                        stack = stackForPop,
                        type = backstack.navDestination::class,
                        inclusive = backstack.inclusive
                    )

                    if (stackForPop.isEmpty()) {
                        val root = rootOf(sectionForPop)
                        if (root != null) stackForPop += root
                    }
                }
            }

            is NavOptions.Backstack.None -> Unit
        }

        ensureInitializedWithAncestors(targetSection)
        val stack = stackFor(targetSection)

        val root = rootOf(targetSection)
        val isRootNavigation = root != null && navDestination::class == root::class

        // restoreState: only meaningful/safe for re-selecting the section root
        if (navOptions.restoreState && isRootNavigation) {
            val saved = savedStacks[targetSection]
            if (saved != null && saved.isNotEmpty() && saved.first()::class == root::class) {
                sectionStacks[targetSection] = saved.toMutableList()
                savedStacks.remove(targetSection)

                activateSection(targetSection)
                updateState()
                return
            }
        }

        val currentTop = stack.lastOrNull()
        if (navOptions.singleTop && currentTop == navDestination) {
            activateSection(targetSection)
            updateState()
            return
        }

        // Avoid duplicating root destination if user navigates to the root explicitly
        if (isRootNavigation && stack.size == 1 && stack.lastOrNull()?.let { it::class } == root?.let { it::class }) {
            activateSection(targetSection)
            updateState()
            return
        }

        stack += navDestination
        activateSection(targetSection)
        updateState()
    }

    override fun switchTo(section: NavSection) {
        lastEvent = NavigationEvent.SwitchTo

        if (!sectionRoots.containsKey(section)) {
            updateState()
            return
        }

        ensureInitializedWithAncestors(section)
        activateSection(section)
        updateState()
    }

    override fun navigateUp() {
        lastEvent = NavigationEvent.NavigateUp

        var section = _state.value.currentSection ?: run {
            updateState()
            return
        }

        while (true) {
            val stack = sectionStacks[section]
            if (stack != null && stack.size > 1) {
                stack.removeLast()
                break
            }

            val parent = parentOf(section) ?: break
            section = parent
        }

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

        val section = sectionOf(navDestination) ?: run {
            updateState()
            return
        }

        ensureInitializedWithAncestors(section)
        val stack = sectionStacks[section] ?: stackFor(section)

        popToTypeInStack(
            stack = stack,
            type = navDestination::class,
            inclusive = inclusive
        )

        if (stack.isEmpty()) {
            val root = rootOf(section)
            if (root != null) stack += root
        }

        activateSection(section)
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
