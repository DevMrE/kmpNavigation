package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Navigation

/**
 * Returns the global [Navigation] instance.
 *
 * This is the same instance that your ViewModels receive from DI if you
 * expose [GlobalNavigation.navigation] as a singleton.
 *
 * ```kotlin
 * @Composable
 * fun BottomBarComponent() {
 *     val navigation = rememberNavigation()
 *
 *     NavigationBarItem(
 *         selected = ...,
 *         onClick = { navigation.switchTo(HomeSection) },
 *         icon = { /* ... */ }
 *     )
 * }
 * ```
 */
@Composable
fun rememberNavigation(): Navigation = GlobalNavigation.navigation

/**
 * Returns the current [NavDestination] from the global navigation state.
 *
 * @param initialDestination Optional fallback while no destination has been set yet.
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
 *     TopAppBarContent(title = stringResource(titleRes))
 * }
 * ```
 */
@Composable
fun rememberNavDestination(
    initialDestination: NavDestination? = null
): NavDestination? {
    val state by GlobalNavigation.controller.state.collectAsState()
    return state.currentDestination ?: initialDestination
}
