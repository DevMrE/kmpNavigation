package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.NavTransitionSpec
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph

/**
 * Root-level NavigationContent.
 *
 * Renders the FIRST (outermost shell) destination in the back stack.
 * Place this once at the very top of your app.
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     AppTheme {
 *         RootNavigationContent(modifier = Modifier.fillMaxSize())
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RootNavigationContent(
    modifier: Modifier = Modifier,
    transitionSpec: ((NavDestination, NavDestination) -> ContentTransform)? = null
) {
    val navState by GlobalNavigation.controller.state.collectAsState()
    val current = navState.backStack.firstOrNull() ?: return

    AnimatedContent(
        modifier = modifier,
        targetState = current,
        transitionSpec = {
            transitionSpec?.invoke(initialState, targetState)
                ?: resolveTransition(
                    event = navState.lastEvent,
                    spec = navState.lastTransition,
                    from = initialState,
                    to = targetState
                )
        },
        label = "RootNavigationContent"
    ) { destination ->
        val screen = NavigationGraph.findScreen(destination)
        if (screen == null) {
            Logger.w("RootNavigationContent") {
                "No screen registered for ${destination::class.simpleName}."
            }
            return@AnimatedContent
        }
        // Do NOT use fillMaxSize here – respect the modifier passed from outside
        screen(destination)
    }
}

/**
 * Section-scoped NavigationContent.
 *
 * Renders the destination right after the shell root of section [S]
 * in the back stack. Skips the shell root itself to avoid infinite loops.
 *
 * The modifier is passed directly to AnimatedContent – do NOT wrap in
 * fillMaxSize so that constraints from the parent (e.g. weight(1f)) are respected.
 *
 * ```kotlin
 * @Composable
 * fun AppRootScreen() {
 *     Scaffold(bottomBar = { BottomBar() }) { padding ->
 *         NavigationContent<AppRootSection>(modifier = Modifier.padding(padding))
 *     }
 * }
 *
 * @Composable
 * fun HomeScreen() {
 *     Column {
 *         TabBar()
 *         NavigationContent<HomeSection>(modifier = Modifier.weight(1f))
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
inline fun <reified S : NavSection> NavigationContent(
    modifier: Modifier = Modifier,
    noinline transitionSpec: ((NavDestination, NavDestination) -> ContentTransform)? = null
) {
    val navState by GlobalNavigation.controller.state.collectAsState()

    val shellRootIndex = navState.backStack.indexOfFirst { destination ->
        NavigationGraph.isSectionShellRoot(destination, S::class)
    }

    val current = if (shellRootIndex >= 0 && shellRootIndex + 1 < navState.backStack.size) {
        navState.backStack[shellRootIndex + 1]
    } else {
        return
    }

    AnimatedContent(
        // Pass modifier directly to AnimatedContent so weight(1f) etc. is respected
        modifier = modifier,
        targetState = current,
        transitionSpec = {
            transitionSpec?.invoke(initialState, targetState)
                ?: resolveTransition(
                    event = navState.lastEvent,
                    spec = navState.lastTransition,
                    from = initialState,
                    to = targetState
                )
        },
        label = "NavigationContent<${S::class.simpleName}>"
    ) { destination ->
        val screen = NavigationGraph.findScreen(destination)
        if (screen == null) {
            Logger.w("NavigationContent") {
                "No screen registered for ${destination::class.simpleName}."
            }
            return@AnimatedContent
        }
        // Pass modifier fillMaxSize only to the content box, not AnimatedContent itself
        Box(modifier = Modifier.fillMaxSize()) {
            screen(destination)
        }
    }
}

/**
 * Resolves the [ContentTransform] based on the last navigation event
 * and the transition spec stored in state.
 */
fun resolveTransition(
    event: NavigationEvent,
    spec: NavTransitionSpec,
    from: NavDestination,
    to: NavDestination
): ContentTransform {
    val fromIndex = NavigationGraph.sectionIndexFor(from)
    val toIndex = NavigationGraph.sectionIndexFor(to)

    return when {
        event == NavigationEvent.SwitchTo
                && fromIndex != null
                && toIndex != null
                && fromIndex != toIndex -> spec.toContentTransform()
        else -> fadeIn() togetherWith fadeOut()
    }
}