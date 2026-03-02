package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph

/**
 * Root-level NavigationContent – renders whatever destination is currently
 * active, regardless of section. Place this once at the very top of your app.
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     NavigationContent(modifier = Modifier.fillMaxSize())
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
            val fromIndex = NavigationGraph.sectionIndexFor(initialState)
            val toIndex = NavigationGraph.sectionIndexFor(targetState)
            val isSectionChange = fromIndex != null && toIndex != null && fromIndex != toIndex

            if (lastEvent == NavigationEvent.SwitchTo && isSectionChange) {
                if (toIndex > fromIndex) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                        slideOutHorizontally { it } + fadeOut())
                }
            } else {
                fadeIn().togetherWith(fadeOut())
            }.using(SizeTransform(clip = true))
        },
        label = "NavigationContent"
    ) { destination ->
        val screen = NavigationGraph.findScreen(destination)
            ?: error("No screen registered for ${destination::class.simpleName}. Did you call registerNavigation()?")
        Box(modifier = Modifier.fillMaxSize()) {
            screen(destination)
        }
    }
}

/**
 * Section-scoped NavigationContent – only renders destinations that belong
 * to section [S] or any of its nested sub-sections.
 *
 * Place this inside the shell screen that owns section [S].
 *
 * ```kotlin
 * // Shell screen for AppRootSection (has BottomBar)
 * @Composable
 * fun AppRootScreen() {
 *     Scaffold(
 *         bottomBar = { BottomBar() }
 *     ) { padding ->
 *         NavigationContent<AppRootSection>(modifier = Modifier.padding(padding))
 *     }
 * }
 *
 * // Shell screen for HomeSection (has TabBar)
 * @Composable
 * fun HomeScreen() {
 *     Scaffold(
 *         topBar = { TabBar() }
 *     ) { padding ->
 *         NavigationContent<HomeSection>(modifier = Modifier.padding(padding))
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
inline fun <reified S : NavSection> NavigationContent(
    modifier: Modifier = Modifier
) {
    val navState by GlobalNavigation.controller.state.collectAsState()
    val current = navState.currentDestination ?: return
    val lastEvent = navState.lastEvent

    if (!NavigationGraph.destinationBelongsToSectionScope(current, S::class)) return

    AnimatedContent(
        modifier = modifier,
        targetState = current,
        transitionSpec = {
            val fromIndex = NavigationGraph.sectionIndexFor(initialState)
            val toIndex = NavigationGraph.sectionIndexFor(targetState)
            val isSectionChange = fromIndex != null && toIndex != null && fromIndex != toIndex

            if (lastEvent == NavigationEvent.SwitchTo && isSectionChange) {
                if (toIndex > fromIndex) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                        slideOutHorizontally { it } + fadeOut())
                }
            } else {
                fadeIn().togetherWith(fadeOut())
            }.using(SizeTransform(clip = true))
        },
        label = "NavigationContent<${S::class.simpleName}>"
    ) { destination ->
        val screen = NavigationGraph.findScreen(destination)
            ?: error("No screen registered for ${destination::class.simpleName}. Did you call registerNavigation()?")
        Box(modifier = Modifier.fillMaxSize()) {
            screen(destination)
        }
    }
}