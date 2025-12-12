package com.kmp.navigation.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavigationEvent
import com.kmp.navigation.NavigationGraph
import com.kmp.navigation.NavDestination
import kotlin.collections.listOf

/**
 * Renders the current navigation destination using the registered screens
 * and applies animations based on the last navigation event:
 *
 * * [NavigationEvent.NavigateTo]  -> fade in / fade out
 * * [NavigationEvent.SwitchTo]   -> horizontal slide based on section index
 * * [NavigationEvent.NavigateUp] -> fade
 * * [NavigationEvent.PopBackTo]  -> fade
 *
 * Section indices are derived from the declaration order of `section<...>()`
 * in your `registerNavigation` DSL.
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     // navigation graph has been configured via registerNavigation(...)
 *     Scaffold(
 *         topBar = { TopAppBarComponent() },
 *         bottomBar = { BottomBarComponent() }
 *     ) { padding ->
 *         NavigationContent(modifier = Modifier.padding(padding))
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationContent(
    modifier: Modifier = Modifier
) {
    val navState by GlobalNavigation.controller.state.collectAsState()
    val current = navState.currentDestination ?: return
    val backStack = navState.backStack // List<NavDestination>

    val entryProvider = remember { createEntryProviderWithDsl(NavigationGraph) }

    val entries = remember(backStack) {
        backStack.map { destination ->
            entryProvider(destination)
        }
    }

    NavDisplay(
        entries = entries,
        onBack = {
            GlobalNavigation.controller.navigateUp()
        },
        modifier = modifier
    )
}
