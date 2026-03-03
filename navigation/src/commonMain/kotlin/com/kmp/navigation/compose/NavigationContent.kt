package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import co.touchlab.kermit.Logger
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.NavTransitionSpec
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph

@Composable
fun RootNavigationContent(
    modifier: Modifier = Modifier,
    transitionSpec: ((NavDestination, NavDestination) -> ContentTransform)? = null
) {
    val navState by GlobalNavigation.controller.state.collectAsState()
    val current = navState.backStack.firstOrNull() ?: return

    AnimatedContent(
        modifier = modifier.clipToBounds(),
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
        screen(destination)
    }
}

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
        modifier = modifier.clipToBounds(),
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
        screen(destination)
    }
}

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