package com.kmp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable

/**
 * Type of registration for a destination.
 */
enum class NavDestinationType {
    /** Respects parent bounds, lands in BackStack */
    Content,

    /** Fullscreen – breaks out of parent bounds, lands in BackStack */
    Screen
}

/**
 * Holds the composable content and type for a registered destination.
 */
data class NavScreenData(
    val content: @Composable (NavDestination) -> Unit,
    val type: NavDestinationType,
    val enterTransition: (() -> EnterTransition)? = null,
    val exitTransition: (() -> ExitTransition)? = null
)