package com.kmp.navigation

import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

/**
 * Global registry mapping destinations to their content and groups.
 */
object NavigationGraph {

    // screens and contents: destination KClass → NavScreenData
    private val destinations =
        mutableMapOf<KClass<out NavDestination>, NavScreenData>()

    // tabs groups: group KClass → NavTabsData
    private val tabGroups =
        mutableMapOf<KClass<out NavGroup>, NavTabsData>()

    // reverse lookup: destination KClass → group KClass it belongs to
    private val destinationToGroup =
        mutableMapOf<KClass<out NavDestination>, KClass<out NavGroup>>()

    private var configured = false

    internal fun configure(
        destinationMap: Map<KClass<out NavDestination>, NavScreenData>,
        tabGroupMap: Map<KClass<out NavGroup>, NavTabsData>,
        destToGroup: Map<KClass<out NavDestination>, KClass<out NavGroup>>
    ) {
        destinations.clear()
        tabGroups.clear()
        destinationToGroup.clear()

        destinations.putAll(destinationMap)
        tabGroups.putAll(tabGroupMap)
        destinationToGroup.putAll(destToGroup)

        configured = true
        Logger.i("NavigationGraph") {
            "Configured with ${destinations.size} destinations, ${tabGroups.size} tab groups."
        }
    }

    /**
     * Find screen data for a destination.
     */
    fun findScreen(destination: NavDestination): NavScreenData? =
        destinations[destination::class]

    /**
     * Find the tabs group a destination belongs to, if any.
     */
    internal fun groupOf(destination: NavDestination): KClass<out NavGroup>? =
        destinationToGroup[destination::class]

    /**
     * Find tabs data for a group.
     */
    internal fun tabsDataFor(groupClass: KClass<out NavGroup>): NavTabsData? =
        tabGroups[groupClass]

    /**
     * Returns true if the destination belongs to a tabs group.
     */
    internal fun isTabDestination(destination: NavDestination): Boolean =
        destinationToGroup.containsKey(destination::class)

    /**
     * Returns the start destination for a group.
     */
    internal fun startDestinationFor(groupClass: KClass<out NavGroup>): NavDestination? =
        tabGroups[groupClass]?.startDestination

    /**
     * Returns all destinations belonging to a group.
     */
    fun destinationsFor(groupClass: KClass<out NavGroup>): List<NavDestination> =
        tabGroups[groupClass]?.destinations ?: emptyList()

    /**
     * Returns the type of destination.
     */
    internal fun typeOf(destination: NavDestination): NavDestinationType? =
        destinations[destination::class]?.type
}