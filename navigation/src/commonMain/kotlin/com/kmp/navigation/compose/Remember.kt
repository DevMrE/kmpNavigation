package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.Navigation
import com.kmp.navigation.ScreenStrategy
import com.kmp.navigation.ScreenStrategyType

@Composable
fun rememberNavigation(): Navigation = remember { GlobalNavigation.navigation }

/**
 * Leaf-most destination (deepest active section + its top destination).
 */
@Composable
fun rememberNavDestination(
    initialDestination: NavDestination? = null
): NavDestination {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentDestination
        ?: initialDestination
        ?: error(
            "No current NavDestination available and no initialDestination provided. " +
                    "Make sure you configured the navigation graph and start destination " +
                    "before calling rememberNavDestination()."
        )
}

/**
 * Leaf-most active section (useful for nested sections like TabBar).
 * For bottom bar selection prefer [rememberActiveChildSection(parentSection)].
 */
@Composable
fun rememberNavSection(
    initialSection: NavSection? = null
): NavSection {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentSection
        ?: initialSection
        ?: error(
            "No current NavSection available and no initialSection provided. " +
                    "Make sure you configured the navigation graph and start destination " +
                    "before calling rememberNavSection()."
        )
}

@Composable
fun rememberRootNavSection(
    initialSection: NavSection? = null
): NavSection {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.rootSection
        ?: initialSection
        ?: error("No root NavSection available and no initialSection provided.")
}

@Composable
fun rememberActiveChildSection(
    parentSection: NavSection,
    initialChild: NavSection? = null
): NavSection {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.activeChild[parentSection]
        ?: initialChild
        ?: error(
            "No active child for parent ${parentSection::class.simpleName} " +
                    "and no initialChild provided."
        )
}

@Composable
fun rememberNavSectionPath(): List<NavSection> {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentPath
}

/**
 * Screen strategy access (available when you use NavigationContent(), because it wraps ProvideScreenStrategy()).
 */
@Composable
fun rememberScreenStrategyType(): ScreenStrategyType = LocalScreenStrategyType.current

@Composable
fun rememberScreenStrategy(): ScreenStrategy = LocalScreenStrategy.current

@Composable
fun rememberScreenSizeDp(): ScreenSizeDp = LocalScreenSizeDp.current
