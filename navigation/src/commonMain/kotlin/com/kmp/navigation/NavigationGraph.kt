package com.kmp.navigation

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

/**
 * Global registry mapping destinations to their Composable content
 * and tracking section hierarchy.
 */
object NavigationGraph {

    private val screens =
        mutableMapOf<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>()
    private val destinationSections =
        mutableMapOf<KClass<out NavDestination>, NavSection>()
    private val sectionRoots =
        mutableMapOf<NavSection, NavDestination>()
    private val sectionIndices =
        mutableMapOf<NavSection, Int>()
    private val sectionParents =
        mutableMapOf<NavSection, NavSection>()

    private var configured = false

    fun isConfigured(): Boolean = configured

    fun configureNavigationGraph(
        startDestination: NavDestination,
        builder: RegisterNavigationBuilder.() -> Unit
    ) {
        screens.clear()
        destinationSections.clear()
        sectionRoots.clear()
        sectionIndices.clear()
        sectionParents.clear()

        var nextIndex = 0

        val dsl = RegisterNavigationBuilder(
            registerScreen = { key, content ->
                if (screens.containsKey(key)) {
                    Logger.w("NavigationGraph") { "${key.simpleName} already registered – skipping." }
                } else {
                    screens[key] = content
                }
            },
            registerDestinationSection = { key, section ->
                destinationSections[key] = section
            },
            registerSectionRoot = { section, root, parent, _ ->
                sectionRoots[section] = root
                if (!sectionIndices.containsKey(section)) {
                    sectionIndices[section] = nextIndex++
                }
                if (parent != null) {
                    sectionParents[section] = parent
                }
            }
        )

        dsl.builder()

        configured = true

        GlobalNavigation.controller.configureSections(
            destinationToSection = destinationSections.toMap(),
            sectionRoots = sectionRoots.toMap(),
            parentSections = sectionParents.toMap(),
            sectionIndices = sectionIndices.toMap()
        )

        val controller = GlobalNavigation.controller
        if (controller.backStack.isEmpty()) {
            val startSection = destinationSections[startDestination::class]
            if (startSection != null) {
                controller.switchTo(startSection)
            } else {
                controller.backStack.add(startDestination)
            }
        }
    }

    fun findScreen(
        destination: NavDestination
    ): (@Composable (NavDestination) -> Unit)? =
        screens[destination::class]

    fun isSectionShellRoot(
        destination: NavDestination,
        sectionClass: KClass<out NavSection>
    ): Boolean {
        val section = sectionRoots.keys.firstOrNull { it::class == sectionClass }
            ?: return false
        val root = sectionRoots[section] ?: return false
        return destination::class == root::class
    }

    internal fun sectionIndexFor(destination: NavDestination): Int? {
        val section = destinationSections[destination::class] ?: return null
        return sectionIndices[section]
    }

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