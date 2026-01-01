package com.kmp.navigation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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

private val LocalNavSaveableStateHolder =
    staticCompositionLocalOf<SaveableStateHolder?> { null }

@Stable
private data class NavSaveableKey(
    val section: NavSection,
    val destination: NavDestination
)

/**
 * Root host: rendert die aktuell aktive Root-Section (Graph-Root)
 * und stellt über SaveableStateHolder State-Restoration über Section-Wechsel sicher.
 */
@Composable
fun RootSectionHost(
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
) {
    if (!NavigationGraph.isConfigured()) {
        fallbackContent()
        return
    }

    val state by GlobalNavigation.controller.state.collectAsState()
    val root = state.rootSection
    if (root == null) {
        fallbackContent()
        return
    }

    val holder = rememberSaveableStateHolder()

    androidx.compose.runtime.CompositionLocalProvider(
        LocalNavSaveableStateHolder provides holder
    ) {
        NavSectionHost(
            section = root,
            modifier = modifier,
            fallbackContent = fallbackContent
        )
    }
}

/**
 * Host für genau eine Section (zeigt deren Stack via NavDisplay).
 *
 * Wichtig: Wir nutzen einen (geteilten) SaveableStateHolder und wrappen jedes Ziel in
 * SaveableStateProvider(section+destination). Dadurch bleibt Compose-UI-State erhalten,
 * auch wenn diese Section gerade nicht sichtbar ist.
 */
@Composable
fun NavSectionHost(
    section: NavSection,
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
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
        fallbackContent()
        return
    }

    val holder = LocalNavSaveableStateHolder.current ?: rememberSaveableStateHolder()

    val provider = remember(section, holder) {
        entryProvider {
            entry<NavDestination> { destination ->
                val screen = NavigationGraph.findScreen(destination)
                    ?: error(
                        "No screen registered for destination " +
                                "${destination::class.simpleName}. Did you call registerNavigation()?"
                    )

                holder.SaveableStateProvider(NavSaveableKey(section, destination)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        screen(destination)
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
 * Host für Child-Sections eines Parents (z.B. BottomBar -> Home/Settings/Auth,
 * oder TabBar -> Movie/Series).
 *
 * Du rufst das in deinem Parent-Root-Screen auf, dort wo die Child-Section erscheinen soll.
 */
@Composable
fun NavChildSectionsHost(
    parentSection: NavSection,
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
) {
    val state by GlobalNavigation.controller.state.collectAsState()
    val children = remember(parentSection) { NavigationGraph.childrenOf(parentSection) }
    val active = state.activeChild[parentSection] ?: children.firstOrNull()

    if (active == null) {
        fallbackContent()
        return
    }

    NavSectionHost(
        section = active,
        modifier = modifier,
        fallbackContent = fallbackContent
    )
}

/**
 * Backwards-compatible Alias (falls du im App-Code weiter NavigationContent() nutzt).
 */
@Composable
fun NavigationContent(
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
) {
    RootSectionHost(modifier = modifier, fallbackContent = fallbackContent)
}
