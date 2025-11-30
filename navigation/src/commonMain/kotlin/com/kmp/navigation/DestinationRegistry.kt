package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kmp.navigation.compose.RegisterNavigation
import kotlin.reflect.KClass

/**
 * Internal registry that maps [NavDestination] types to their Composable content.
 *
 * This is filled by the `section { screen<Destination> { ... } }` DSL used
 * inside [RegisterNavigation].
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
     * Render the given [destination] using the registered Composable.
     *
     * ```kotlin
     * registry.Render(HomeScreenDestination)
     * ```
     */
    @Composable
    fun Render(destination: NavDestination) {
        val screen = screens[destination::class]
            ?: error("No screen registered for destination ${destination::class.simpleName}.")
        screen(destination)
    }
}

/**
 * CompositionLocal holding the [DestinationRegistry] for the current navigation tree.
 *
 * You normally do not access this directly. Instead it is consumed by [com.kmp.navigation.compose.NavigationHost].
 */
val LocalDestinationRegistry = staticCompositionLocalOf<DestinationRegistry> {
    error("No DestinationRegistry found. Make sure you are inside RegisterNavigation.")
}
