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
import com.kmp.navigation.NavSection
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph

/**
 * Root-level NavigationContent – renders the outermost shell destination.
 *
 * Always renders the FIRST entry of the back stack, which is the top-level
 * shell screen (e.g. AppRootContent).
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     AppTheme {
 *         RootNavigationContent(modifier = Modifier.fillMaxSize())
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RootNavigationContent(
    modifier: Modifier = Modifier
) {
    val navState by GlobalNavigation.controller.state.collectAsState()
    val current = navState.backStack.firstOrNull() ?: return
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
        label = "RootNavigationContent"
    ) { destination ->
        val screen = NavigationGraph.findScreen(destination)
            ?: error("No screen registered for ${destination::class.simpleName}. Did you call registerNavigation()?")
        Box(modifier = Modifier.fillMaxSize()) {
            screen(destination)
        }
    }
}

/**
 * Section-scoped NavigationContent – renders the active destination within
 * section [S], skipping the shell root of [S] to avoid infinite loops.
 *
 * Finds the shell root of [S] in the back stack, then renders the next
 * entry after it.
 *
 * ```kotlin
 * @Composable
 * fun AppRootContent() {
 *     Scaffold(bottomBar = { BottomBar() }) { padding ->
 *         NavigationContent<AppRootSection>(modifier = Modifier.padding(padding))
 *     }
 * }
 *
 * @Composable
 * fun HomeScreen() {
 *     Column {
 *         TabBar()
 *         NavigationContent<HomeScreenSection>()
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
    val lastEvent = navState.lastEvent

    val shellRootIndex = navState.backStack.indexOfFirst { destination ->
        NavigationGraph.isSectionShellRoot(destination, S::class)
    }

    val current = if (shellRootIndex >= 0 && shellRootIndex + 1 < navState.backStack.size) {
        navState.backStack[shellRootIndex + 1]
    } else return

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