package com.kmp.navigation.compose

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import co.touchlab.kermit.Logger
import com.kmp.navigation.*

@Composable
fun RootNavigationContent(
    modifier: Modifier = Modifier,
    transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()

    val topLevelSection = navState.backStack
        .mapNotNull { controller.sectionOf(it) }
        .firstOrNull { NavigationGraph.parentSectionOf(it) == null }
        ?: return

    Logger.i("KmpNavigation", message = { "Root Navigation: $topLevelSection" })

    RenderSection(
        section = topLevelSection,
        modifier = modifier,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec
    )
}

@Composable
inline fun <reified S : NavSection> NavigationContent(
    modifier: Modifier = Modifier,
    noinline transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    noinline popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    noinline predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null
) {
    val sectionInstance = remember {
        NavigationGraph.sectionInstanceFor(S::class)
    }

    if (sectionInstance == null) {
        Logger.w("KmpNavigation") { "No section instance found for ${S::class.simpleName}." }
        return
    }

    RenderSection(
        section = sectionInstance,
        modifier = modifier,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec
    )
}

@Composable
@PublishedApi
internal fun RenderSection(
    section: NavSection,
    modifier: Modifier = Modifier,
    transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()
    val lastEvent = navState.lastEvent

    val subStack = remember(section) { mutableStateListOf<NavDestination>() }

    LaunchedEffect(navState.backStack) {
        val newStack = controller.subStackFor(section)
        subStack.clear()
        subStack.addAll(newStack)
    }

    if (subStack.isEmpty()) return

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

    val defaultPredictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform =
        {
            slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
        }

    NavDisplay(
        modifier = modifier,
        backStack = subStack,
        onBack = { GlobalNavigation.navigation.navigateUp() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator()
        ),
        transitionSpec = transitionSpec ?: defaultTransitionSpec,
        popTransitionSpec = popTransitionSpec ?: defaultPopTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec
            ?: defaultPredictivePopTransitionSpec,
        // Lambda format statt DSL – unterstützt generische NavDestination Basisklasse
        entryProvider = { destination ->
            val screenData = NavigationGraph.findScreenWithMetadata(destination)
            if (screenData == null) {
                Logger.w("NavigationContent") {
                    "No screen registered for ${destination::class.simpleName}."
                }
                // Leerer Fallback – kein Crash
                NavEntry(key = destination) {}
            } else {
                NavEntry(key = destination) {
                    screenData.content(destination)
                }
            }
        }
    )
}