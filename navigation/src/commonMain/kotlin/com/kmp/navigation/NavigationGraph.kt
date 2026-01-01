package com.kmp.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Global registry mapping [NavDestination] -> screen content
 * + graph metadata for nested sections.
 */
object NavigationGraph {

    private val screens =
        mutableMapOf<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>()

    // destination type -> section instance
    private val destinationSections =
        mutableMapOf<KClass<out NavDestination>, NavSection>()

    // section instance -> root destination instance
    private val sectionRoots =
        mutableMapOf<NavSection, NavDestination>()

    // child -> parent (null = root section)
    private val sectionParents =
        mutableMapOf<NavSection, NavSection?>()

    // parent -> children (declaration order)
    private val sectionChildren =
        mutableMapOf<NavSection, MutableList<NavSection>>()

    // section -> index among siblings (under its parent)
    private val sectionSiblingIndex =
        mutableMapOf<NavSection, Int>()

    private var configured: Boolean = false

    fun isConfigured(): Boolean = configured

    fun configureNavigationGraph(
        startDestination: NavDestination,
        builder: RegisterNavigationBuilder.() -> Unit
    ) {
        screens.clear()
        destinationSections.clear()
        sectionRoots.clear()
        sectionParents.clear()
        sectionChildren.clear()
        sectionSiblingIndex.clear()

        val nextIndexPerParent = mutableMapOf<NavSection?, Int>()

        val dsl = RegisterNavigationBuilder(
            registerScreen = { key, screen ->
                if (screens.put(key, screen) != null) {
                    error("Destination ${key.simpleName} is already registered.")
                }
            },
            registerDestinationSection = { destKey, section ->
                destinationSections[destKey] = section
            },
            registerSectionRoot = { section, root ->
                if (sectionRoots.put(section, root) != null) {
                    error("Section ${section::class.simpleName} already has a root registered.")
                }
            },
            registerSectionParent = { section, parent ->
                if (sectionParents.containsKey(section)) {
                    error("NavSection ${section::class.simpleName} is already registered in the graph.")
                }
                sectionParents[section] = parent

                val idx = nextIndexPerParent[parent] ?: 0
                nextIndexPerParent[parent] = idx + 1
                sectionSiblingIndex[section] = idx

                if (parent != null) {
                    sectionChildren.getOrPut(parent) { mutableListOf() }.add(section)
                }
            }
        )

        dsl.builder()
        configured = true

        GlobalNavigation.controller.configureGraph(
            destinationToSection = destinationSections.toMap(),
            sectionRoots = sectionRoots.toMap(),
            sectionParents = sectionParents.toMap(),
            sectionChildren = sectionChildren.mapValues { it.value.toList() },
            sectionSiblingIndex = sectionSiblingIndex.toMap()
        )

        // set initial destination once
        val controller = GlobalNavigation.controller
        if (controller.state.value.currentDestination == null) {
            GlobalNavigation.navigation.navigateTo(startDestination) {
                clearStack()
            }
        }
    }

    internal fun findScreen(destination: NavDestination): (@Composable (NavDestination) -> Unit)? =
        screens[destination::class]

    internal fun parentOf(section: NavSection): NavSection? = sectionParents[section]

    internal fun childrenOf(section: NavSection): List<NavSection> = sectionChildren[section].orEmpty()

    internal fun rootSections(): List<NavSection> =
        sectionParents
            .filterValues { it == null }
            .keys
            .sortedBy { sectionSiblingIndex[it] ?: 0 }

    internal fun sectionIndexFor(destination: NavDestination): Int? {
        val section = destinationSections[destination::class] ?: return null
        return sectionSiblingIndex[section]
    }
}
