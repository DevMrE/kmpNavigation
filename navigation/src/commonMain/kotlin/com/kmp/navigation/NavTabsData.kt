package com.kmp.navigation

import kotlin.reflect.KClass

/**
 * Holds configuration for a registered tabs group.
 */
data class NavTabsData(
    val tabsClass: KClass<out NavTabs>,
    val startDestination: NavDestination,
    val destinations: List<NavDestination>
)