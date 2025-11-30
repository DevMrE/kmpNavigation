package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph
import com.kmp.navigation.NavDestination

/**
 * Renders the current navigation destination using the registered screens
 * and applies animations based on the last navigation event:
 *
 * * [NavigationEvent.NavigateTo]  -> fade in / fade out
 * * [NavigationEvent.SwitchTo]   -> horizontal slide based on section index
 * * [NavigationEvent.NavigateUp] -> fade
 * * [NavigationEvent.PopBackTo]  -> fade
 *
 * Section indices are derived from the declaration order of `section<...>()`
 * in your `registerNavigation` DSL.
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     // navigation graph has been configured via registerNavigation(...)
 *     Scaffold(
 *         topBar = { TopAppBarComponent() },
 *         bottomBar = { BottomBarComponent() }
 *     ) { padding ->
 *         NavigationContent(modifier = Modifier.padding(padding))
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationContent(
    modifier: Modifier = Modifier
) {
    val navState by GlobalNavigation.controller.state.collectAsState()
    val current = navState.currentDestination ?: return
    val lastEvent = navState.lastEvent

    AnimatedContent(
        modifier = modifier,
        targetState = current,
        transitionSpec = {
            // Decide animation type based on lastEvent & section indices
            val fromIndex: Int? = initialState.let { NavigationGraph.sectionIndexFor(it) }
            val toIndex: Int? = targetState.let { NavigationGraph.sectionIndexFor(it) }

            val isSectionChange =
                fromIndex != null && toIndex != null && fromIndex != toIndex

            if (lastEvent == NavigationEvent.SwitchTo && isSectionChange) {
                // Horizontal slide between sections
                if (toIndex > fromIndex) {
                    // Going "right": new section has higher index
                    (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()).togetherWith(
                        slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
                } else {
                    // Going "left": new section has lower index
                    (slideInHorizontally { fullWidth -> -fullWidth } + fadeIn()).togetherWith(
                        slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
                }
            } else {
                // Default for navigateTo / back / popBackTo: simple fade
                fadeIn().togetherWith(fadeOut())
            }.using(
                SizeTransform(clip = true)
            )
        },
        label = "NavigationContent"
    ) { destination: NavDestination ->
        val screen = NavigationGraph.findScreen(destination)
            ?: error("No screen registered for destination ${destination::class.simpleName}. Did you call registerNavigation()?")

        Box(modifier = Modifier.fillMaxSize()) {
            screen(destination)
        }
    }
}
