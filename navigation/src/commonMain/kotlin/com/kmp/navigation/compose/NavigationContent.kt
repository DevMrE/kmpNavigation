package com.kmp.navigation.compose

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import co.touchlab.kermit.Logger
import com.kmp.navigation.*

/**
 * Root-level NavigationContent.
 *
 * Automatically detects the active top-level section and renders it via NavDisplay.
 * Place this once at the very top of your app.
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
@Composable
fun RootNavigationContent(
    modifier: Modifier = Modifier,
    predictiveBackEnabled: Boolean = true,
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

    RenderSection(
        section = topLevelSection,
        modifier = modifier,
        predictiveBackEnabled = predictiveBackEnabled,
        transitionSpec = transitionSpec,
        popTransitionSpec = popTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec
    )
}

/**
 * Section-scoped NavigationContent backed by NavDisplay.
 *
 * Renders only destinations belonging to section [S].
 * Layout constraints from outside (e.g. weight(1f)) are fully respected.
 * Scroll position and UI state are preserved across section switches via SaveableStateHolder.
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
 *     Column(modifier = Modifier.fillMaxSize()) {
 *         TabBar()
 *         NavigationContent<HomeScreenSection>(modifier = Modifier.weight(1f))
 *     }
 * }
 * ```
 */
@Composable
inline fun <reified S : NavSection> NavigationContent(
    modifier: Modifier = Modifier,
    predictiveBackEnabled: Boolean = true,
    noinline transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    noinline popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    noinline predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null
) {
    val sectionInstance = remember {
        NavigationGraph.sectionInstanceFor(S::class)
    }

    if (sectionInstance == null) {
        Logger.w("NavigationContent") { "No section instance found for ${S::class.simpleName}." }
        return
    }

    RenderSection(
        section = sectionInstance,
        modifier = modifier,
        predictiveBackEnabled = predictiveBackEnabled,
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
    predictiveBackEnabled: Boolean = true,
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
        onBack = if (predictiveBackEnabled) {
            { GlobalNavigation.navigation.navigateUp() }
        } else {
            {}
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator()
        ),
        transitionSpec = transitionSpec ?: defaultTransitionSpec,
        popTransitionSpec = popTransitionSpec ?: defaultPopTransitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec
            ?: defaultPredictivePopTransitionSpec,
        entryProvider = entryProvider {
            entry<NavDestination> { destination ->
                val screenData = NavigationGraph.findScreenWithMetadata(destination)
                if (screenData == null) {
                    Logger.w("NavigationContent") {
                        "No screen registered for ${destination::class.simpleName}."
                    }
                    return@entry
                }
                screenData.content(destination)
            }
        }
    )
}