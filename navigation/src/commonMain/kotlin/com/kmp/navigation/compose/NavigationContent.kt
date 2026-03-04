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

/**
 * Renders the active destination for section [S].
 *
 * @param isRoot If true, renders fullscreen with its own layout scope (for top-level sections
 *               like AppRootSection or DetailNavSection that have their own Scaffold).
 *               If false (default), respects parent layout constraints
 *               (e.g. weight(1f) inside a Column).
 *
 * ```kotlin
 * // Top-level sections – fullscreen
 * @Composable
 * fun AppScreen() {
 *     AppTheme {
 *         NavigationContent<AppRootSection>(isRoot = true)
 *         NavigationContent<DetailNavSection>(isRoot = true)
 *     }
 * }
 *
 * // Child sections – respects parent constraints
 * @Composable
 * fun AppRootContent() {
 *     Scaffold(bottomBar = { BottomBar() }) { padding ->
 *         NavigationContent<AppRootSection>(
 *             modifier = Modifier.padding(padding)
 *         )
 *     }
 * }
 *
 * @Composable
 * fun HomeScreen() {
 *     Column(modifier = Modifier.fillMaxSize()) {
 *         Tabs()
 *         NavigationContent<HomeScreenSection>(modifier = Modifier.weight(1f))
 *     }
 * }
 * ```
 */
@Composable
inline fun <reified S : NavSection> NavigationContent(
    modifier: Modifier = Modifier,
    isRoot: Boolean = false,
    noinline transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    noinline popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    noinline predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null
) {
    val sectionInstance = remember {
        NavigationGraph.sectionInstanceFor(S::class)
    }

    if (sectionInstance == null) {
        Logger.w("KmpNavigation") {
            "No section instance found for ${S::class.simpleName}."
        }
        return
    }

    RenderSection(
        section = sectionInstance,
        modifier = modifier,
        isRoot = isRoot,
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
    isRoot: Boolean = false,
    transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
    predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null
) {
    val controller = GlobalNavigation.controller
    val navState by controller.state.collectAsState()
    val lastEvent = navState.lastEvent

    if (isRoot) {
        val topLevelSection = navState.backStack
            .mapNotNull { controller.sectionOf(it) }
            .firstOrNull { NavigationGraph.parentSectionOf(it) == null }

        if (topLevelSection != section) {
            Logger.d("KmpNavigation") {
                "renderSection(${section::class.simpleName}, isRoot=true): " +
                        "not active top-level section (active: ${topLevelSection?.let { it::class.simpleName }}). Skipping."
            }
            return
        }
    }

    val subStack = remember(section, navState.backStack) {
        mutableStateListOf<NavDestination>().also {
            it.addAll(controller.subStackFor(section))
        }
    }

    Logger.i("KmpNavigation") {
        "renderSection(${section::class.simpleName}, isRoot=$isRoot) subStack: $subStack"
    }

    if (subStack.isEmpty()) {
        Logger.w("KmpNavigation") {
            "renderSection(${section::class.simpleName}): subStack is empty – nothing to render."
        }
        return
    }

    val defaultTransitionSpec: AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform = {
        when (lastEvent) {
            NavigationEvent.SwitchTo -> fadeIn() togetherWith fadeOut()
            else -> slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        }
    }

    val defaultPopTransitionSpec: AnimatedContentTransitionScope<Scene<NavDestination>>.() ->
    ContentTransform = {
        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
    }

    val defaultPredictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) ->
    ContentTransform = {
        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
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
        entryProvider = { destination ->
            NavEntry(key = destination) {
                val screenData = NavigationGraph.findScreenWithMetadata(destination)
                if (screenData == null) {
                    Logger.w("KmpNavigation") {
                        "No screen registered for ${destination::class.simpleName}."
                    }
                    return@NavEntry
                }

                screenData.content(destination)
            }
        }
    )
}