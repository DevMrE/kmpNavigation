package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import co.touchlab.kermit.Logger
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph

@Composable
inline fun <reified S : NavSection> NavigationContent(
    modifier: Modifier = Modifier,
    noinline transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    noinline popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    noinline predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()
    val navigation = GlobalNavigation.navigation

    val sectionInstance = remember {
        NavigationGraph.sectionInstanceFor(S::class)
    }

    if (sectionInstance == null) {
        Logger.w("NavigationContent") { "No section instance found for ${S::class.simpleName}." }
        return
    }

    val subStack = remember { mutableStateListOf<NavDestination>() }

    LaunchedEffect(navState.backStack) {
        val newStack = controller.subStackFor(sectionInstance)
        subStack.clear()
        subStack.addAll(newStack)
    }

    if (subStack.isEmpty()) return

    val lastEvent = navState.lastEvent

    val defaultTransitionSpec: AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform = {
        when (lastEvent) {
            NavigationEvent.SwitchTo -> fadeIn() togetherWith fadeOut()
            else -> slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        }
    }

    val defaultPopTransitionSpec: AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform = {
        slideInHorizontally { -it } + fadeIn() togetherWith
                slideOutHorizontally { it } + fadeOut()
    }

    val defaultPredictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform = {
        slideInHorizontally { -it } + fadeIn() togetherWith
                slideOutHorizontally { it } + fadeOut()
    }

    NavDisplay(
        modifier = modifier,
        backStack = subStack,
        onBack = { navigation.navigateUp() },
        transitionSpec = transitionSpec ?: defaultTransitionSpec,
        popTransitionSpec = popTransitionSpec ?: defaultPopTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec ?: defaultPredictivePopTransitionSpec,
        entryProvider = entryProvider {
            entry<NavDestination> { destination ->
                val screen = NavigationGraph.findScreen(destination)
                if (screen == null) {
                    Logger.w("NavigationContent") {
                        "No screen registered for ${destination::class.simpleName}."
                    }
                    return@entry
                }
                screen(destination)
            }
        }
    )
}