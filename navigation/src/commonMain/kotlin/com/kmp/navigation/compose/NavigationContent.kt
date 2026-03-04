package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import co.touchlab.kermit.Logger
import com.kmp.navigation.*

@PublishedApi
internal object RootSentinel : NavDestination

/**
 * Root-level navigation host.
 * Place this ONCE at the very top of your app.
 *
 * Handles:
 * - `screen` destinations fullscreen via NavDisplay
 * - Back navigation on all platforms via NavDisplay.onBack
 * - Optional [exitScreen] composable shown when BackStack is empty and user navigates back
 *
 * ```kotlin
 * @Composable
 * fun MobileAppScreen() {
 *     AppTheme {
 *         NavigationRoot(
 *             exitScreen = { onConfirm ->
 *                 ExitConfirmDialog(onConfirm = onConfirm)
 *             }
 *         ) {
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

    val navDisplayBackStack = remember(navState.backStack) {
        mutableStateListOf<NavDestination>().also { list ->
            // Sentinel is always present – NavDisplay requires non-empty backStack
            list.add(RootSentinel)
            list.addAll(
                navState.backStack.filter { dest ->
                    NavigationGraph.findTabs(dest) == null
                }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (showExitScreen && exitScreen != null) {
            exitScreen.invoke {
                controller.navigateUp()
                showExitScreen = false
            }
        }

        NavDisplay(
            modifier = if (isScreenOnTop) Modifier.fillMaxSize() else Modifier,
            backStack = navDisplayBackStack,
            onBack = {
                val nonTabSize = navState.backStack.count { NavigationGraph.findTabs(it) == null }
                if (nonTabSize == 0) {
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
                            Logger.w("KmpNavigation") { "No screen for ${destination::class.simpleName}." }
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
 * Animations are resolved per destination from the registration.
 * If no animation is defined for a destination, default animations are used.
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
) {
    val controller = NavigationFactory.controller()
    val navState by controller.state.collectAsState()
    val tabsClass = T::class

    val activeDestination = remember(navState.backStack, navState.lastEvent) {
        controller.activeDestinationFor(tabsClass)
    } ?: run {
        Logger.w("NavigationTabs") { "NavigationTabs<${T::class.simpleName}>: no active destination found." }
        return
    }

    val lastEvent = navState.lastEvent

    AnimatedContent(
        modifier = modifier.clipToBounds(),
        targetState = activeDestination,
        transitionSpec = {
            val targetData = NavigationGraph.findScreen(targetState)
            val initialData = NavigationGraph.findScreen(initialState)
            val enter = targetData?.enterTransition?.invoke()
            val exit = initialData?.exitTransition?.invoke()

            if (enter != null && exit != null) enter togetherWith exit
            else when (lastEvent) {
                NavigationEvent.SwitchTab ->
                    DefaultNavAnimations.tabSwitchTransition

                NavigationEvent.NavigateUp,
                NavigationEvent.PopBackTo ->
                    DefaultNavAnimations.popEnterTransition

                else -> DefaultNavAnimations.enterTransition
            }
        },
        label = "KmpNavigation<${T::class.simpleName}>"
    ) { destination ->
        HandleDestination(destination)
    }
}

/**
 * Renders a specific content destination from the BackStack.
 * Respects parent bounds via [modifier].
 * Only renders when [D] is the current non-tab content destination on top of the BackStack.
 *
 * Animations are resolved per destination from the registration.
 * If no animation is defined for a destination, default animations are used.
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
) {
    val controller = NavigationFactory.controller()
    val navState by controller.state.collectAsState()

    val currentDestination = navState.backStack.lastOrNull {
        it is D && NavigationGraph.findTabs(it) == null && NavigationGraph.typeOf(it) == NavDestinationType.Content
    }

    var lastKnownDestination by remember { mutableStateOf(currentDestination) }

    if (currentDestination != null) {
        lastKnownDestination = currentDestination
    }

    val currentEvent = controller.state.value.lastEvent
    val currentDestinationData = lastKnownDestination?.let { NavigationGraph.findScreen(it) }

    AnimatedVisibility(
        modifier = modifier.clipToBounds(),
        visible = currentDestination != null,
        enter = currentDestinationData?.enterTransition?.invoke() ?: when (currentEvent) {
            NavigationEvent.NavigateUp,
            NavigationEvent.PopBackTo -> slideInHorizontally { -it } + fadeIn()
            else -> slideInHorizontally { it } + fadeIn()
        },
        exit = currentDestinationData?.exitTransition?.invoke() ?: when (currentEvent) {
            NavigationEvent.NavigateUp,
            NavigationEvent.PopBackTo -> slideOutHorizontally { it } + fadeOut()
            else -> slideOutHorizontally { -it } + fadeOut()
        }
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val targetData = targetState?.let { NavigationGraph.findScreen(it) }
                val initialData = initialState?.let { NavigationGraph.findScreen(it) }
                val event = controller.state.value.lastEvent
                val enter = targetData?.enterTransition?.invoke()
                val exit = initialData?.exitTransition?.invoke()

                if (enter != null && exit != null) enter togetherWith exit
                else when (event) {
                    NavigationEvent.NavigateUp,
                    NavigationEvent.PopBackTo -> DefaultNavAnimations.popEnterTransition

                    else -> DefaultNavAnimations.enterTransition
                }
            },
            label = "KmpNavigation<${D::class.simpleName}>"
        ) { destination ->
            HandleDestination(destination)
        }
    }
}

@Composable
@PublishedApi
internal fun HandleDestination(destination: NavDestination?) {
    if (destination == null) return
    val data = NavigationGraph.findScreen(destination)
    if (data == null) {
        Logger.w("KmpNavigation") {
            "No screen for ${destination::class.simpleName}."
        }
        return
    }
    data.content(destination)
}