package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kmp.navigation.compose.NavigationController
import org.koin.compose.koinInject

/**
 * CompositionLocal holding the current [Navigation] instance.
 *
 * You normally do not access this directly. Instead use [rememberNavigation].
 */
val LocalNavigation = staticCompositionLocalOf<Navigation> {
    error("No Navigation found. Make sure you are inside RegisterNavigation.")
}

/**
 * Provides a [Navigation] instance.
 *
 * Order of precedence:
 * 1. If Koin is available, returns the `single<Navigation>` from your DI setup.
 * 2. Otherwise, creates a local instance via [NavigationFactory.create] and remembers it.
 *
 * ```kotlin
 * // Koin module
 * val navigationModule = module {
 *     single<Navigation> { NavigationFactory.create() }
 * }
 *
 * // Compose - this is used internally by RegisterNavigation
 * @Composable
 * fun AppRoot() {
 *     val navigation: Navigation = provideNavigationInstance()
 * }
 * ```
 */
@Composable
internal fun provideNavigationInstance(): Navigation {
    return runCatching {
        koinInject<Navigation>()
    }.getOrElse {
        remember { NavigationFactory.create() }
    }
}

/**
 * Returns the current [Navigation] instance from the composition.
 *
 * This will always be the same object within a single RegisterNavigation tree.
 *
 * ```kotlin
 * @Composable
 * fun BottomBarComponent() {
 *     val navigation = rememberNavigation()
 *
 *     NavigationBar {
 *         NavigationBarItem(
 *             selected = ...,
 *             onClick = { navigation.switchTab(HomeScreenDestination) },
 *             icon = { Icon(...) }
 *         )
 *     }
 * }
 * ```
 */
@Composable
fun rememberNavigation(): Navigation = LocalNavigation.current

/**
 * Returns the current [NavDestination] from the navigation state.
 *
 * @param initialDestination Optional value used while the navigation state is still empty.
 *
 * This is very handy for top app bars and bottom bars:
 *
 * ```kotlin
 * @Composable
 * fun TopAppBarComponent() {
 *     val navDestination = rememberNavDestination()
 *
 *     val titleRes = when (navDestination) {
 *         is SettingsScreenDestination -> Res.string.settings_screen_title
 *         else -> Res.string.app_name
 *     }
 *
 *     TopAppBar(
 *         title = { Text(stringResource(titleRes)) }
 *     )
 * }
 * ```
 */
@Composable
fun rememberNavDestination(
    initialDestination: NavDestination? = null
): NavDestination? {
    val navigation = rememberNavigation()
    val controller = navigation as? NavigationController
        ?: error("rememberNavDestination() works only with NavigationFactory.create() implementation.")
    val state by controller.state.collectAsState()
    return state.currentDestination ?: initialDestination
}
