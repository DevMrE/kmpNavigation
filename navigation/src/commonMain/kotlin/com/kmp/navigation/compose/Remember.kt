package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.Navigation

@Composable
fun rememberNavigation(): Navigation = remember { GlobalNavigation.navigation }

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
 * Liefert die aktuell sichtbare (deepest) Section.
 * Bei nested Sections ist das z.B. die aktive Tab-Section.
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

/**
 * Root-Section des aktuell aktiven Graphs (z.B. BottomBarNavSection, wenn du so modellierst).
 */
@Composable
fun rememberRootNavSection(
    initialSection: NavSection? = null
): NavSection {
    val state by GlobalNavigation.controller.state.collectAsState()

    return state.rootSection
        ?: initialSection
        ?: error(
            "No root NavSection available and no initialSection provided."
        )
}

/**
 * Aktiver Child einer Parent-Section (für BottomBar/TabBar Auswahl).
 */
@Composable
fun rememberActiveChildSection(
    parentSection: NavSection,
    initialChild: NavSection? = null
): NavSection {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.activeChild[parentSection]
        ?: initialChild
        ?: error("No active child for parent ${parentSection::class.simpleName} and no initialChild provided.")
}

@Composable
fun rememberNavSectionPath(): List<NavSection> {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentPath
}
