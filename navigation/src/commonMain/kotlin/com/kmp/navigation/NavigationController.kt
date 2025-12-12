package com.kmp.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

/**
 * Navigation-Implementierung mit einem eigenen Backstack pro [NavSection].
 *
 * Verhalten (wie bei "multiple back stacks" in Navigation 3 / Bottom Navigation):
 *
 * - Jede [NavSection] besitzt ihren eigenen Stack von [NavDestination]s.
 * - [navigateTo] arbeitet immer auf dem Stack der Section, zu der die Destination gehört.
 * - [switchTo] wechselt nur die aktive Section und stellt deren letzte Destination wieder her,
 *   ohne andere Stacks zu verändern.
 * - [navigateUp] poppt ausschließlich innerhalb der aktuell aktiven Section.
 */
class NavigationController : Navigation {

    /**
     * Snapshot des aktuellen Navigationszustands.
     *
     * Wird von den Compose-Helfern konsumiert:
     * - rememberNavDestination
     * - rememberNavSection
     * - NavigationContent
     */
    data class State(
        val currentDestination: NavDestination? = null,
        val currentSection: NavSection? = null,
        val backStack: List<NavDestination> = emptyList(),
        val lastEvent: NavigationEvent = NavigationEvent.Idle
    )

    // Pro Section ein eigener Backstack
    private val sectionStacks = mutableMapOf<NavSection, MutableList<NavDestination>>()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    // Destination-Typ -> Section
    private var destinationSections: Map<KClass<out NavDestination>, NavSection> = emptyMap()

    // Section -> Root-Destination
    private var sectionRoots: Map<NavSection, NavDestination> = emptyMap()

    // Letzte Operation, die den Backstack verändert hat (für Animationen)
    private var lastEvent: NavigationEvent = NavigationEvent.Idle

    /**
     * Wird von [NavigationGraph.configureNavigationGraph] aufgerufen,
     * nachdem der DSL-Builder ausgeführt wurde.
     */
    internal fun configureSections(
        destinationToSection: Map<KClass<out NavDestination>, NavSection>,
        sectionRoots: Map<NavSection, NavDestination>
    ) {
        this.destinationSections = destinationToSection
        this.sectionRoots = sectionRoots
        // Stacks bleiben unverändert – hier konfigurieren wir nur Metadaten.
    }

    private fun sectionOf(destination: NavDestination): NavSection? =
        destinationSections[destination::class]

    private fun stackFor(section: NavSection): MutableList<NavDestination> =
        sectionStacks.getOrPut(section) { mutableListOf() }

    private val currentSection: NavSection?
        get() = _state.value.currentSection

    private fun updateState(newCurrentSection: NavSection? = currentSection) {
        val section = newCurrentSection
        val stack = section?.let { sectionStacks[it] }.orEmpty()
        val current = stack.lastOrNull()

        _state.value = State(
            currentDestination = current,
            currentSection = section,
            backStack = stack.toList(),
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

        // Backstack-Optionen zuerst anwenden
        when (val backstack = navOptions.backstack) {
            is NavOptions.Backstack.None -> Unit

            is NavOptions.Backstack.Clear -> {
                // Entspricht "alles zurücksetzen", z.B. nach Login
                sectionStacks.clear()
            }

            is NavOptions.Backstack.PopTo -> {
                // Pop in dem Stack, dem die Ziel-Destination gehört
                val sectionForPop =
                    sectionOf(backstack.navDestination)
                        ?: targetSection

                val stackForPop = sectionStacks[sectionForPop]
                if (stackForPop != null) {
                    popToTypeInStack(
                        stack = stackForPop,
                        type = backstack.navDestination::class,
                        inclusive = backstack.inclusive
                    )
                }
            }
        }

        val stack = stackFor(targetSection)

        // restoreState: nicht neu pushen, sondern zum letzten Eintrag gleichen Typs springen
        if (navOptions.restoreState) {
            val idx = stack.indexOfLast { it::class == navDestination::class }
            if (idx >= 0) {
                stack.subList(idx + 1, stack.size).clear()
                updateState(newCurrentSection = targetSection)
                return
            }
        }

        val currentTop = stack.lastOrNull()
        if (navOptions.singleTop && currentTop == navDestination) {
            // Schon oben – nichts ändern
            updateState(newCurrentSection = targetSection)
            return
        }

        stack += navDestination
        updateState(newCurrentSection = targetSection)
    }

    override fun switchTo(section: NavSection) {
        lastEvent = NavigationEvent.SwitchTo

        val stack = stackFor(section)

        if (stack.isEmpty()) {
            // Erste Aktivierung dieser Section → ihren Root verwenden
            val root = sectionRoots[section]
                ?: run {
                    // Keine Info über diese Section, nichts zu tun
                    updateState()
                    return
                }
            stack += root
        }

        // Nur aktive Section wechseln, andere Stacks bleiben unberührt
        updateState(newCurrentSection = section)
    }

    override fun navigateUp() {
        lastEvent = NavigationEvent.NavigateUp

        val section = currentSection
        if (section == null) {
            updateState()
            return
        }

        val stack = sectionStacks[section]
        if (stack == null || stack.size <= 1) {
            // Wir sind auf dem Root dieser Section – hier nichts mehr zu poppen.
            updateState(newCurrentSection = section)
            return
        }

        stack.removeLast()
        updateState(newCurrentSection = section)
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

        val section = sectionOf(navDestination) ?: currentSection
        if (section == null) {
            updateState()
            return
        }

        val stack = sectionStacks[section]
        if (stack == null) {
            updateState(newCurrentSection = section)
            return
        }

        popToTypeInStack(
            stack = stack,
            type = navDestination::class,
            inclusive = inclusive
        )

        if (stack.isEmpty()) {
            // Wenn wir alles weggepoppt haben, auf Root der Section zurückfallen
            val root = sectionRoots[section]
            if (root != null) {
                stack += root
            }
        }

        updateState(newCurrentSection = section)
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
