package com.kmp.navigation

/**
 * Snapshot of the current navigation state.
 */
data class NavigationState(
    val backStack: List<NavDestination> = emptyList(),
    val currentDestination: NavDestination? = null,
    val currentSection: NavSection? = null,
    val lastEvent: NavigationEvent = NavigationEvent.Idle,
    val lastTransition: NavTransitionSpec = NavTransitions.fade
)