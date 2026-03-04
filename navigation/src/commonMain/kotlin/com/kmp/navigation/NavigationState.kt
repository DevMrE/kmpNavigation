package com.kmp.navigation

/**
 * Snapshot of the current navigation state.
 */
data class NavigationState(
    val backStack: List<NavDestination> = emptyList(),
    val currentDestination: NavDestination? = null,
    val lastEvent: NavigationEvent = NavigationEvent.Idle
)