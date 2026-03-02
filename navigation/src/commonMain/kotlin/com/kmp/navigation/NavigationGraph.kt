package com.kmp.navigation

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

object NavigationGraph {

    private val screens = mutableMapOf<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>()
    private val destinationSections = mutableMapOf<KClass<out NavDestination>, NavSection>()
    private val sectionRoots = mutableMapOf<NavSection, NavDestination>()
    private val sectionIndices = mutableMapOf<NavSection, Int>()
    private val sectionParents = mutableMapOf<NavSection, NavSection>()
    private val overlaySections = mutableSetOf<NavSection>()

    private var configured: Boolean = false

    fun isConfigured(): Boolean = configured

    fun debugPrintHierarchy() {
        Logger.i("NavigationGraph") { "=== destinationSections ===" }
        destinationSections.forEach { (k, v) -> Logger.i("NavigationGraph") { "${k.simpleName} -> $v" } }
        Logger.i("NavigationGraph") { "=== sectionParents ===" }
        sectionParents.forEach { (k, v) -> Logger.i("NavigationGraph") { "$k -> $v" } }
        Logger.i("NavigationGraph") { "=== sectionRoots ===" }
        sectionRoots.forEach { (k, v) -> Logger.i("NavigationGraph") { "$k -> $v" } }
    }

    fun configureNavigationGraph(
        startDestination: NavDestination,
        builder: RegisterNavigationBuilder.() -> Unit
    ) {
        screens.clear()
        destinationSections.clear()
        sectionRoots.clear()
        sectionIndices.clear()
        sectionParents.clear()
        overlaySections.clear()

        var nextSectionIndex = 0

        val dsl = RegisterNavigationBuilder(
            registerScreen = { key, screen ->
                if (screens.put(key, screen) != null) {
                    error("Destination ${key.simpleName} is already registered.")
                }
            },
            registerDestinationSection = { destKey, section ->
                destinationSections[destKey] = section
            },
            registerSectionRoot = { section, root, parentSection, isOverlay ->
                sectionRoots[section] = root
                if (!sectionIndices.containsKey(section)) {
                    sectionIndices[section] = nextSectionIndex++
                }
                if (parentSection != null) {
                    sectionParents[section] = parentSection
                }
                if (isOverlay) {
                    overlaySections += section
                }
            }
        )
        dsl.builder()

        configured = true

        GlobalNavigation.controller.configureSections(
            destinationToSection = destinationSections.toMap(),
            sectionRoots = sectionRoots.toMap()
        )

        if (GlobalNavigation.controller.state.value.currentDestination == null) {
            GlobalNavigation.navigation.navigateTo(startDestination) { clearStack() }
        }
    }

    fun findScreen(
        destination: NavDestination
    ): (@Composable (NavDestination) -> Unit)? = screens[destination::class]

    fun sectionIndexFor(destination: NavDestination): Int? {
        val section = destinationSections[destination::class] ?: return null
        return sectionIndices[section]
    }

    fun destinationBelongsToSectionScope(
        destination: NavDestination,
        sectionClass: KClass<out NavSection>
    ): Boolean {
        val destSection = destinationSections[destination::class] ?: return false
        return isSectionOrDescendant(destSection, scopeClass = sectionClass)
    }

    /**
     * Returns true if [destination] is the shell root of [sectionClass].
     *
     * The shell root is the root destination of the section itself – the one
     * whose registered screen contains NavigationContent<S>. It must not be
     * rendered by NavigationContent<S> itself to avoid infinite loops.
     */
    fun isSectionShellRoot(
        destination: NavDestination,
        sectionClass: KClass<out NavSection>
    ): Boolean {
        // Find the section instance for sectionClass
        val section = sectionRoots.keys.firstOrNull { it::class == sectionClass } ?: return false
        val root = sectionRoots[section] ?: return false
        return destination::class == root::class
    }

    internal fun isOverlaySection(section: NavSection): Boolean =
        section in overlaySections

    internal fun parentSectionOf(section: NavSection): NavSection? =
        sectionParents[section]

    private fun isSectionOrDescendant(
        candidate: NavSection,
        scopeClass: KClass<out NavSection>
    ): Boolean {
        if (candidate::class == scopeClass) return true
        val parent = sectionParents[candidate] ?: return false
        return isSectionOrDescendant(parent, scopeClass)
    }
}