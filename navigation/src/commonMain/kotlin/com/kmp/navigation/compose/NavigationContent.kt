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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.kmp.navigation.NavigationFactory
import com.kmp.navigation.NavigationGraph

/**
 * Root-level navigation host.
 * Place this ONCE at the very top of your app.
 *
 * Handles:
 * - `screen` destinations fullscreen via NavDisplay
 * - Back navigation on all platforms via NavDisplay.onBack
 *
 * ```kotlin
 * @Composable
 * fun MobileAppScreen() {
 *     AppTheme {
 *         NavigationRoot {
 *             AppContent()
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    exitScreen: @Composable ((onConfirm: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()
    var showExitScreen by remember { mutableStateOf(false) }

    val isScreenOnTop = navState.backStack.lastOrNull()?.let {
        NavigationGraph.typeOf(it) == NavDestinationType.Screen
    } ?: false

    val fullBackStack = remember(navState.backStack) {
        mutableStateListOf<NavDestination>().also {
            it.addAll(navState.backStack)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (showExitScreen && exitScreen != null) {
            exitScreen.invoke {
                // onConfirm – navigateUp intern aufgerufen
                controller.navigateUp()
                showExitScreen = false
            }
        }

        NavDisplay(
            modifier = if (isScreenOnTop) Modifier.fillMaxSize() else Modifier,
            backStack = fullBackStack,
            onBack = {
                if (navState.backStack.size <= 1) {
                    showExitScreen = true
                } else {
                    controller.navigateUp()
                }
            },
            entryProvider = { destination ->
                NavEntry(key = destination) {
                    if (isScreenOnTop) {
                        val data = NavigationGraph.findScreen(destination)
                        if (data == null) {
                            Logger.w("NavigationRoot") {
                                "No screen for ${destination::class.simpleName}."
                            }
                            return@NavEntry
                        }
                        data.content(destination)
                    }
                }
            }
        )
    }
}

/**
 * Renders the currently active destination within a tabs group [T].
 * Respects parent bounds via [modifier].
 * Does NOT handle back navigation – NavigationRoot handles that.
 *
 * ```kotlin
 * // In AppContent:
 * NavigationTabs<BottomBarTabs>(Modifier.fillMaxSize())
 *
 * // In HomeContent:
 * NavigationTabs<HomeTabs>(Modifier.weight(1f))
 * ```
 */
@Composable
inline fun <reified T : NavTabs> NavigationTabs(
    modifier: Modifier = Modifier,
    noinline transitionSpec: (() -> ContentTransform)? = null
) {
    val controller = NavigationFactory.controller()
    val navState by controller.state.collectAsState()
    val tabsClass = T::class

    val activeDestination = remember(navState.backStack, navState.lastEvent) {
        controller.activeDestinationFor(tabsClass)
    } ?: run {
        Logger.w("NavigationTabs") {
            "NavigationTabs<${T::class.simpleName}>: no active destination found."
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
        label = "NavigationTabs<${T::class.simpleName}>"
    ) { destination ->
        val data = NavigationGraph.findScreen(destination)
        if (data == null) {
            Logger.w("NavigationTabs") {
                "No screen for ${destination::class.simpleName}."
            }
            return@AnimatedContent
        }
        data.content(destination)
    }
}

/**
 * Renders a specific content destination from the BackStack.
 * Respects parent bounds via [modifier].
 * Only renders when [D] is the current non-tab content destination on top of the BackStack.
 *
 * ```kotlin
 * Box(Modifier.fillMaxSize().padding(padding)) {
 *     NavigationTabs<BottomBarTabs>(Modifier.fillMaxSize())
 *     NavigationContent<PopularMovieDestination>(Modifier.fillMaxSize())
 * }
 * ```
 */
@Composable
inline fun <reified D : NavDestination> NavigationContent(
    modifier: Modifier = Modifier,
    noinline transitionSpec: (() -> ContentTransform)? = null
) {
    val controller = NavigationFactory.controller()
    val navState by controller.state.collectAsState()

    val currentDestination = navState.backStack.lastOrNull {
        it is D &&
                NavigationGraph.findTabs(it) == null &&
                NavigationGraph.typeOf(it) == NavDestinationType.Content
    } ?: return

    val lastEvent = navState.lastEvent

    val defaultTransitionSpec: () -> ContentTransform = {
        when (lastEvent) {
            NavigationEvent.NavigateUp,
            NavigationEvent.PopBackTo -> slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
            else -> slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        }
    }

    AnimatedContent(
        modifier = modifier.clipToBounds(),
        targetState = currentDestination,
        transitionSpec = { transitionSpec?.invoke() ?: defaultTransitionSpec() },
        label = "NavigationContent<${D::class.simpleName}>"
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