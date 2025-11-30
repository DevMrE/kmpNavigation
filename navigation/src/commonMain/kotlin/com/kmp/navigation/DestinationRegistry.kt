package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kmp.navigation.compose.NavigationContent
import kotlin.reflect.KClass

/**
 * Internal registry that maps [NavDestination] types to their Composable content.
 *
 * This is filled by the `section { screen<Destination> { ... } }` DSL used
 * inside [registerNavigation].
 *
 * ```kotlin
 * @Composable
 * fun AppScreen() {
 *     RegisterNavigation(
 *         startDestination = HomeScreenDestination,
 *         builder = {
 *             section<HomeSection, HomeScreenDestination> {
 *                 screen<HomeScreenDestination> { HomeScreen() }
 *                 screen<SettingsScreenDestination> { SettingsScreen() }
 *             }
 *         }
 *     ) {
 *         NavigationHost()
 *     }
 * }
 * ```
 */
class DestinationRegistry internal constructor(
    internal val screens: Map<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>
) {

    /**
     * Render the given [navDestination] using the registered Composable.
     *
     * ```kotlin
     * registry.Render(HomeScreenDestination)
     * ```
     */
    @Composable
    fun Render(navDestination: NavDestination) {
        val screen = screens[navDestination::class]
            ?: error("No screen registered for destination ${navDestination::class.simpleName}.")
        screen(navDestination)
    }
}

/**
 * CompositionLocal holding the [DestinationRegistry] for the current navigation tree.
 *
 * You normally do not access this directly. Instead it is consumed by [NavigationContent].
 */
val LocalDestinationRegistry = staticCompositionLocalOf<DestinationRegistry> {
    error("No DestinationRegistry found. Make sure you are inside RegisterNavigation.")
}
