package com.kmp.navigation

import kotlin.reflect.KClass

/**
 * Holds configuration for a registered tabs group.
 */
data class NavTabsData(
    val groupClass: KClass<out NavGroup>,
    val startDestination: NavDestination,
    val destinations: List<NavDestination>
)