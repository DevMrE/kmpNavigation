package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.NavSection
import com.kmp.navigation.Navigation

/**
 * Returns the singleton [Navigation] instance used by the library.
 *
 * You can inject [Navigation] into view models via DI or use this function
 * directly in composables when passing the instance around would be overkill.
 *
 * ```kotlin
 * @Composable
 * fun BottomBarComponent() {
 *     val navigation = rememberNavigation()
 *
 *     NavigationBarItem(
 *         selected = /* ... */,
 *         onClick = { navigation.switchTo(HomeSection) },
 *         icon = { /* ... */ }
 *     )
 * }
 * ```
 */
@Composable
fun rememberNavigation(): Navigation {
    // GlobalNavigation.navigation is already a singleton, we just
    // wrap it in remember so the reference is stable for Compose.
    return remember { GlobalNavigation.navigation }
}

/**
 * Observes the current [NavDestination] from the global navigation state.
 *
 * If there is no active destination yet (for example before the first
 * navigation action), [initialDestination] is returned instead.
 *
 * ```kotlin
 * @Composable
 * fun TopAppBarComponent() {
 *     val destination = rememberNavDestination(initialDestination = HomeScreenDestination)
 *
 *     val title = when (destination) {
 *         is SettingsScreenDestination -> stringResource(Res.string.settings_screen_title)
 *         else -> stringResource(Res.string.app_name)
 *     }
 *
 *     MyTopAppBar(title = title)
 * }
 * ```
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
 * Observes the current navigation section as a concrete [NavSection] instance.
 *
 * This assumes that your sections are implemented as singletons, for example:
 *
 * ```kotlin
 * @Serializable
 * data object HomeSection : NavSection
 * ```
 *
 * Usage in a bottom bar:
 *
 * ```kotlin
 * @Composable
 * fun BottomBarComponent() {
 *     val navigation = rememberNavigation()
 *     val section = rememberNavSection(initialSection = HomeSection)
 *
 *     NavigationBar {
 *         NavigationBarItem(
 *             selected = section == HomeSection,
 *             onClick = { navigation.switchTo(HomeSection) },
 *             icon = { /* ... */ }
 *         )
 *
 *         NavigationBarItem(
 *             selected = section == SettingsSection,
 *             onClick = { navigation.switchTo(SettingsSection) },
 *             icon = { /* ... */ }
 *         )
 *     }
 * }
 * ```
 *
 * @param initialSection Optional fallback value that is returned when
 * there is no current section yet (for example very early in app startup).
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
