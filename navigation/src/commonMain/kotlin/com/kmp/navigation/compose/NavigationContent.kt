package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import co.touchlab.kermit.Logger
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestinationType
import com.kmp.navigation.NavTabs
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph

/**
 * Renders the currently active destination within a tabs group [NavTabs].
 *
 * - Respects parent bounds via [modifier]
 * - Animates between destinations
 * - For `screen` type destinations, renders fullscreen regardless of modifier
 *
 * ```kotlin
 * // In AppScreen:
 * NavigationContent<AppRoot>(Modifier.padding(padding))
 *
 * // In HomeContent:
 * NavigationContent<HomeTabs>(Modifier.weight(1f))
 * ```
 */
@Composable
inline fun <reified NG : NavTabs> NavigationContent(
    modifier: Modifier = Modifier,
    noinline transitionSpec: (() -> ContentTransform)? = null
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()
    val groupClass = NG::class

    // Get active destination for this group
    val activeDestination = remember(navState.backStack, navState.lastEvent) {
        controller.activeDestinationFor(groupClass)
    } ?: run {
        Logger.w("NavigationContent") {
            "NavigationContent<${NG::class.simpleName}>: no active destination found."
        }
        return
    }

    val screenData = NavigationGraph.findScreen(activeDestination) ?: run {
        Logger.w("NavigationContent") {
            "NavigationContent<${NG::class.simpleName}>: " +
                    "no screen registered for ${activeDestination::class.simpleName}."
        }
        return
    }

    val lastEvent = navState.lastEvent

    val defaultTransitionSpec: () -> ContentTransform = {
        when (lastEvent) {
            NavigationEvent.SwitchTab -> fadeIn() togetherWith fadeOut()
            NavigationEvent.NavigateUp,
            NavigationEvent.PopBackTo -> slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
            else -> slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        }
    }

    // For screen type: render fullscreen, ignore modifier bounds
    // For content type: respect modifier bounds
    val isFullscreen = screenData.type == NavDestinationType.Screen

    val contentModifier = if (isFullscreen) {
        Modifier.fillMaxSize().clipToBounds()
    } else {
        modifier.clipToBounds()
    }

    AnimatedContent(
        modifier = contentModifier,
        targetState = activeDestination,
        transitionSpec = { transitionSpec?.invoke() ?: defaultTransitionSpec() },
        label = "NavigationContent<${NG::class.simpleName}>"
    ) { destination ->
        val data = NavigationGraph.findScreen(destination)
        if (data == null) {
            Logger.w("NavigationContent") {
                "No screen for ${destination::class.simpleName}"
            }
            return@AnimatedContent
        }
        data.content(destination)
    }
}

/**
 * Renders the current top of the BackStack.
 *
 * Use this to render screen destinations that are pushed on top of the
 * normal tab navigation (e.g. DetailScreen, PopularMovieScreen).
 *
 * Place this ONCE at the very top of your app, wrapping your root content.
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     AppTheme {
 *         // Renders DetailScreen on top when navigated to
 *         ScreenNavigationHost {
 *             // Normal tab content below
 *             AppRootContent { padding ->
 *                 NavigationContent<AppRoot>(Modifier.padding(padding))
 *             }
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun ScreenNavigationHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()

    val topDestination = navState.backStack.lastOrNull()
    val isScreenOnTop = topDestination?.let {
        NavigationGraph.typeOf(it) == NavDestinationType.Screen
    } ?: false

    Box(modifier = modifier.fillMaxSize()) {
        // Always render the base content below
        content()

        // If a screen destination is on top, render it over everything
        if (isScreenOnTop) {
            val screenData = NavigationGraph.findScreen(topDestination)
            if (screenData != null) {
                AnimatedContent(
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                    targetState = topDestination,
                    transitionSpec = {
                        val lastEvent = navState.lastEvent
                        when (lastEvent) {
                            NavigationEvent.NavigateUp,
                            NavigationEvent.PopBackTo -> slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                            else -> slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        }
                    },
                    label = "ScreenNavigationHost"
                ) { destination ->
                    val data = NavigationGraph.findScreen(destination)
                    if (data != null) {
                        data.content(destination)
                    }
                }
            }
        }
    }
}