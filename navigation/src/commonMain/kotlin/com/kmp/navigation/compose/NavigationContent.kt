package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import co.touchlab.kermit.Logger
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
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
inline fun <reified T : NavTabs> NavigationContent(
    modifier: Modifier = Modifier,
    noinline transitionSpec: (() -> ContentTransform)? = null
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()
    val tabsClass = T::class

    val topDestination = navState.backStack.lastOrNull()
    val isScreenOnTop = topDestination?.let {
        NavigationGraph.typeOf(it) == NavDestinationType.Screen
    } ?: false

    if (isScreenOnTop) {
        val screenBackStack = remember(navState.backStack) {
            mutableStateListOf<NavDestination>().also { list ->
                list.addAll(
                    navState.backStack.filter {
                        NavigationGraph.typeOf(it) == NavDestinationType.Screen
                    }
                )
            }
        }

        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = screenBackStack,
            onBack = { GlobalNavigation.navigation.navigateUp() },
            entryProvider = { destination ->
                NavEntry(key = destination) {
                    val data = NavigationGraph.findScreen(destination)
                    if (data == null) {
                        Logger.w("NavigationContent") {
                            "No screen for ${destination::class.simpleName}."
                        }
                        return@NavEntry
                    }
                    data.content(destination)
                }
            }
        )
        return
    }

    val activeDestination = remember(navState.backStack, navState.lastEvent) {
        controller.activeDestinationFor(tabsClass)
    } ?: run {
        Logger.w("NavigationContent") {
            "NavigationContent<${T::class.simpleName}>: no active destination found."
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

    AnimatedContent(
        modifier = modifier.clipToBounds(),
        targetState = activeDestination,
        transitionSpec = { transitionSpec?.invoke() ?: defaultTransitionSpec() },
        label = "NavigationContent<${T::class.simpleName}>"
    ) { destination ->
        val data = NavigationGraph.findScreen(destination)
        if (data == null) {
            Logger.w("NavigationContent") {
                "No screen for ${destination::class.simpleName}."
            }
            return@AnimatedContent
        }
        data.content(destination)
    }
}