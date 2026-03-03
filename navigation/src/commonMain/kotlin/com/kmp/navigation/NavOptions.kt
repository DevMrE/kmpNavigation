package com.kmp.navigation

/**
 * Options to influence how a navigation action is performed.
 */
data class NavOptions(
    /**
     * If true, don't push if the destination is already on top.
     */
    var singleTop: Boolean = true,

    /**
     * Custom transition for this navigation action.
     * If null, the default transition based on NavigationEvent is used.
     */
    var transition: NavTransitionSpec? = null
)