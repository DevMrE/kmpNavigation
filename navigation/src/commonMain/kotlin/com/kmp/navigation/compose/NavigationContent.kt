package com.kmp.navigation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.NavigationGraph
import com.kmp.navigation.ScreenRole

private val LocalNavSaveableStateHolder =
    staticCompositionLocalOf<SaveableStateHolder?> { null }

@Stable
private data class NavSaveableKey(
    val section: NavSection,
    val destination: NavDestination
)

/**
 * Main entry point for consumers.
 *
 * IMPORTANT:
 * - With multi-sections, you typically do NOT wrap this into a Scaffold with bottomBar.
 * - Instead: create a RootHostDestination screen and use [AdaptiveSectionScaffold] there.
 *
 * This composable:
 * - provides the current ScreenStrategy via [ProvideScreenStrategy]
 * - renders the active ROOT section via [RootSectionHost]
 */
@Composable
fun NavigationContent(
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
) {
    if (!NavigationGraph.isConfigured()) {
        fallbackContent()
        return
    }

    ProvideScreenStrategy(modifier = modifier.fillMaxSize()) {
        RootSectionHost(
            modifier = Modifier.fillMaxSize(),
            fallbackContent = fallbackContent
        )
    }
}

/**
 * Root host: renders the active root section and provides a shared SaveableStateHolder
 * so each destination keeps its rememberSaveable state across section switches.
 */
@Composable
fun RootSectionHost(
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
) {
    val state by GlobalNavigation.controller.state.collectAsState()
    val root = state.rootSection

    if (root == null) {
        fallbackContent()
        return
    }

    val holder = rememberSaveableStateHolder()

    CompositionLocalProvider(
        LocalNavSaveableStateHolder provides holder
    ) {
        NavSectionHost(
            section = root,
            modifier = modifier,
        )
    }
}

/**
 * Host for a single section: renders its stack via Navigation3 NavDisplay.
 *
 * - Uses SaveableStateProvider(section+destination) for state restoration
 * - Supports optional two-pane: if top destination has role Detail and current ScreenStrategy allows it
 */
@Composable
fun NavSectionHost(
    section: NavSection,
    modifier: Modifier = Modifier,
) {
    val navigation = rememberNavigation()
    val state by GlobalNavigation.controller.state.collectAsState()

    val stackSnapshot = state.backStacks[section].orEmpty()
    val backStack = remember(section) { mutableStateListOf<NavDestination>() }

    LaunchedEffect(stackSnapshot) {
        backStack.clear()
        backStack.addAll(stackSnapshot)
    }

    if (backStack.isEmpty()) {
        return
    }

    val holder = LocalNavSaveableStateHolder.current ?: rememberSaveableStateHolder()

    // Keep latest values for the remembered provider closure
    val currentStackState = rememberUpdatedState(stackSnapshot)
    val currentStrategyState = rememberUpdatedState(LocalScreenStrategy.current)
    val currentSizeState = rememberUpdatedState(LocalScreenSizeDp.current)

    val provider = remember(section, holder) {
        entryProvider {
            entry<NavDestination> { destination ->
                val screen = NavigationGraph.findScreen(destination)
                    ?: error(
                        "No screen registered for destination " +
                                "${destination::class.simpleName}. Did you call registerNavigation()?"
                    )

                val stack = currentStackState.value
                val strategy = currentStrategyState.value
                val size = currentSizeState.value

                val isTop = stack.lastOrNull() == destination
                val isDetail = NavigationGraph.roleOf(destination) == ScreenRole.Detail

                val twoPaneAllowed =
                    strategy.twoPane.enabled &&
                            (strategy.twoPane.minWidthDp <= 0f || size.widthDp >= strategy.twoPane.minWidthDp)

                // Two-pane rendering: show previous entry as master + this entry as detail
                if (twoPaneAllowed && isTop && isDetail && stack.size >= 2) {
                    val master = stack[stack.size - 2]
                    val masterScreen = NavigationGraph.findScreen(master)
                        ?: error("No screen registered for master destination ${master::class.simpleName}")

                    Row(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .weight(strategy.twoPane.primaryPaneFraction)
                                .fillMaxHeight()
                        ) {
                            holder.SaveableStateProvider(NavSaveableKey(section, master)) {
                                masterScreen(master)
                            }
                        }
                        Box(
                            Modifier
                                .weight(1f - strategy.twoPane.primaryPaneFraction)
                                .fillMaxHeight()
                        ) {
                            holder.SaveableStateProvider(NavSaveableKey(section, destination)) {
                                screen(destination)
                            }
                        }
                    }
                } else {
                    holder.SaveableStateProvider(NavSaveableKey(section, destination)) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            screen(destination)
                        }
                    }
                }
            }
        }
    }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = { navigation.navigateUp() },
        entryProvider = provider
    )
}

/**
 * Host for a parent's active child section.
 *
 * Used inside a parent root destination screen (e.g. bottom bar host or tab host).
 */
@Composable
fun NavChildSectionsHost(
    parentSection: NavSection,
    modifier: Modifier = Modifier,
) {
    val state by GlobalNavigation.controller.state.collectAsState()
    val children = remember(parentSection) { NavigationGraph.childrenOf(parentSection) }
    val active = state.activeChild[parentSection] ?: children.firstOrNull()

    if (active == null) {
        return
    }

    NavSectionHost(
        section = active,
        modifier = modifier,
    )
}
