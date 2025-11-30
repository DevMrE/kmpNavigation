package com.kmp.navigation.compose

import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavOptions
import com.kmp.navigation.NavSection
import com.kmp.navigation.Navigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Navigation implementation with a **single global back stack** and
 * section-aware switching.
 *
 * * [navigateTo] pushes destinations on a global stack.
 * * [switchTo] uses "last screen per section" + the section root to decide
 *   which destination to show and pushes it as a new history entry.
 * * [navigateUp] always goes to the previously shown screen, even if it
 *   belongs to a different section.
 *
 * Example behavior:
 *
 * 1. HomeSection - MovieScreenDestination
 * 2. navigateTo(SeriesScreenDestination)    -> HomeSection - Series
 * 3. switchTo<AuthSection>()                -> AuthSection - Login
 * 4. navigateUp()                           -> HomeSection - Series
 */
class NavigationController : Navigation {

    data class State(
        val currentDestination: NavDestination? = null,
        val currentSection: KClass<out NavSection>? = null,
        val backStack: List<NavDestination> = emptyList()
    )

    private val backStack = mutableListOf<NavDestination>()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // destination type -> section type
    private var destinationSections: Map<KClass<out NavDestination>, KClass<out NavSection>> =
        emptyMap()

    // section type -> root destination type
    private var sectionRootDestinationClasses: Map<KClass<out NavSection>, KClass<out NavDestination>> =
        emptyMap()

    // section type -> last visited destination of that section
    private val lastDestinationPerSection: MutableMap<KClass<out NavSection>, NavDestination> =
        mutableMapOf()

    /**
     * Configure section information.
     *
     * Called by the navigation graph builder after the DSL has been evaluated.
     */
    internal fun configureSections(
        destinationToSection: Map<KClass<out NavDestination>, KClass<out NavSection>>
    ) {
        this.destinationSections = destinationToSection
    }

    private fun sectionOf(destination: NavDestination): KClass<out NavSection>? =
        destinationSections[destination::class]

    private fun rootDestinationOf(section: KClass<out NavSection>): NavDestination? {
        val rootClass = sectionRootDestinationClasses[section] ?: return null
        return rootClass as? NavDestination
    }

    private fun updateState() {
        val current = backStack.lastOrNull()
        val currentSection = current?.let { sectionOf(it) }
        _state.value = State(
            currentDestination = current,
            currentSection = currentSection,
            backStack = backStack.toList()
        )
    }

    override fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit
    ) {
        val navOptions = NavOptions().apply(options)

        // Handle back stack options (global stack)
        when (val backstack = navOptions.backstack) {
            is NavOptions.Backstack.None -> Unit
            is NavOptions.Backstack.Clear -> {
                backStack.clear()
            }
            is NavOptions.Backstack.PopTo -> {
                popToTypeInStack(backStack, backstack.navDestination::class, backstack.inclusive)
            }
        }

        // restoreState: jump back to last destination of same type instead of pushing
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

        // singleTop: avoid pushing exact same instance
        if (navOptions.singleTop && current == navDestination) {
            updateState()
            return
        }

        backStack += navDestination
        sectionOf(navDestination)?.let { section ->
            lastDestinationPerSection[section] = navDestination
        }
        updateState()
    }

    override fun <S : NavSection> switchTo(section: KClass<S>) {
        val sectionKey: KClass<out NavSection> = section

        // 1) Try last visited destination of that section
        val target = lastDestinationPerSection[sectionKey]
        // 2) fall back to section root
            ?: rootDestinationOf(sectionKey)
            ?: error("No root destination configured for section ${sectionKey.simpleName}.")

        val current = backStack.lastOrNull()
        if (current == target) {
            // Already on that screen – no-op
            updateState()
            return
        }

        backStack += target
        sectionOf(target)?.let { s ->
            lastDestinationPerSection[s] = target
        }
        updateState()
    }

    override fun navigateUp() {
        if (backStack.size <= 1) return
        backStack.removeLast()
        updateState()
    }

    override fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean
    ) {
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
