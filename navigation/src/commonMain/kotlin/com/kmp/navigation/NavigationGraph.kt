package com.kmp.navigation

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

object NavigationGraph {

    private val screens =
        mutableMapOf<KClass<out NavDestination>, NavScreenData>()
    private val destinationSections =
        mutableMapOf<KClass<out NavDestination>, NavSection>()
    private val sectionRoots =
        mutableMapOf<NavSection, NavDestination>()
    private val sectionDefaults =
        mutableMapOf<NavSection, NavDestination>()
    private val sectionIndices =
        mutableMapOf<NavSection, Int>()
    private val sectionParents =
        mutableMapOf<NavSection, NavSection>()

    fun configureNavigationGraph(
        startDestination: NavDestination,
        builder: RegisterNavigationBuilder.() -> Unit
    ) {
        screens.clear()
        destinationSections.clear()
        sectionRoots.clear()
        sectionDefaults.clear()
        sectionIndices.clear()
        sectionParents.clear()

        var nextIndex = 0

        val dsl = RegisterNavigationBuilder(
            registerScreen = { key, screenData ->
                if (screens.containsKey(key)) {
                    Logger.w("NavigationGraph") {
                        "${key.simpleName} already registered – skipping."
                    }
                } else {
                    screens[key] = screenData
                }
            },
            registerDestinationSection = { key, section ->
                destinationSections[key] = section
            },
            registerSectionRoot = { section, root, default, parent ->
                sectionRoots[section] = root
                if (default != null) sectionDefaults[section] = default
                if (!sectionIndices.containsKey(section)) {
                    sectionIndices[section] = nextIndex++
                }
                if (parent != null) sectionParents[section] = parent
            }
        )

        dsl.builder()

        Logger.i("NavigationGraph") {
            "Configured sections: ${sectionRoots.keys.map { it::class.simpleName }}"
        }
        Logger.i("NavigationGraph") {
            "Section defaults: ${sectionDefaults.map { "${it.key::class.simpleName} → ${it.value::class.simpleName}" }}"
        }
        Logger.i("NavigationGraph") {
            "Section parents: ${sectionParents.map { "${it.key::class.simpleName} → ${it.value::class.simpleName}" }}"
        }

        GlobalNavigation.controller.configureSections(
            destinationToSection = destinationSections.toMap(),
            sectionRoots = sectionRoots.toMap(),
            sectionDefaults = sectionDefaults.toMap(),
            parentSections = sectionParents.toMap(),
            sectionIndices = sectionIndices.toMap()
        )

        if (GlobalNavigation.controller.backStack.isEmpty()) {
            GlobalNavigation.controller.buildInitialStack(startDestination)
        }
    }

    internal fun findScreen(
        destination: NavDestination
    ): (@Composable (NavDestination) -> Unit)? = screens[destination::class]?.content


    fun sectionInstanceFor(
        sectionClass: KClass<out NavSection>
    ): NavSection? =
        sectionRoots.keys.firstOrNull { it::class == sectionClass }

    internal fun parentSectionOf(section: NavSection): NavSection? =
        sectionParents[section]

    fun destinationBelongsToSectionScope(
        destination: NavDestination,
        sectionClass: KClass<out NavSection>
    ): Boolean {
        val destSection = destinationSections[destination::class] ?: return false
        return isSectionOrDescendant(destSection, sectionClass)
    }

    private fun isSectionOrDescendant(
        candidate: NavSection,
        scopeClass: KClass<out NavSection>
    ): Boolean {
        if (candidate::class == scopeClass) return true
        val parent = sectionParents[candidate] ?: return false
        return isSectionOrDescendant(parent, scopeClass)
    }
}