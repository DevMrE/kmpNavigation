package com.kmp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.navigation3.scene.Scene

/**
 * Holds the composable content and optional per-destination transition metadata.
 */
data class NavScreenData(
    val content: @Composable (NavDestination) -> Unit,
    val transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    val popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    val predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null
)