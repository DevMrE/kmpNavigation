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
 * Place this ONCE at the very top of your app.
 * Handles all screen destinations fullscreen + back navigation via NavDisplay.
 * Tab content is rendered via NavigationContent<T> within your Scaffold.
 *
 * ```kotlin
 * @Composable
 * fun MobileAppScreen() {
 *     AppTheme {
 *         NavigationHost {
 *             AppContent()
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun NavigationHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()

    // Full backStack for NavDisplay – handles back navigation on all platforms
    val fullBackStack = remember(navState.backStack) {
        mutableStateListOf<NavDestination>().also {
            it.addAll(navState.backStack)
        }
    }

    val isScreenOnTop = navState.backStack.lastOrNull()?.let {
        NavigationGraph.typeOf(it) == NavDestinationType.Screen
    } ?: false

    Box(modifier = modifier.fillMaxSize()) {
        // Always render tab content below
        content()

        // NavDisplay handles screen destinations + back navigation
        if (isScreenOnTop) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = fullBackStack,
                onBack = { GlobalNavigation.navigation.navigateUp() },
                entryProvider = { destination ->
                    NavEntry(key = destination) {
                        val data = NavigationGraph.findScreen(destination)
                        if (data == null) {
                            Logger.w("NavigationHost") {
                                "No screen for ${destination::class.simpleName}."
                            }
                            return@NavEntry
                        }
                        data.content(destination)
                    }
                }
            )
        }
    }
}

/**
 * Renders the currently active destination within a tabs group [T].
 * Respects parent bounds via [modifier].
 *
 * ```kotlin
 * // In AppContent:
 * NavigationContent<BottomBarTabs>(Modifier.padding(padding))
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