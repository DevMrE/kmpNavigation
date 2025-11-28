package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kmp.navigation.LocalNavigator
import com.kmp.navigation.NavDestination
import com.kmp.navigation.TypedGraph
import com.kmp.navigation.TypedGraphBuilder
import com.kmp.navigation.install

/**
 * Sets up a typed `RegisterNavigation` and provides the app navigator to the composition.
 *
 * You MUST return a [TypedGraph] from [content] using the typed entry
 * point `navGraph { ... }`. Inside that block register destinations with
 * [TypedGraphBuilder.screen] or nested graphs with [TypedGraphBuilder.section].
 *
 * Parameters
 * - `startNavDestination`: initial typed destination instance (your `NavDestination`).
 * - `modifier`: optional modifier passed to `RegisterNavigation`.
 * - `content`: a builder that returns a [com.kmp.navigation.TypedGraph] via `navGraph { ... }`.
 *
 * Usage — single screen
 * ```kotlin
 * @kotlinx.serialization.Serializable
 * data object Settings : NavDestination
 *
 * @Composable
 * fun App() {
 *     RegisterNavigation(startNavDestination = Settings) {
 *         // Build and return a TypedGraph
 *         navGraph {
 *             screen<Settings> {
 *                 SettingsScreen()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * Usage — section (nested navigation)
 * ```kotlin
 * @kotlinx.serialization.Serializable
 * data object Home : NavDestination              // parent graph route
 *
 * @kotlinx.serialization.Serializable
 * data object Movies : NavDestination            // child A (start)
 *
 * @kotlinx.serialization.Serializable
 * data object Series : NavDestination            // child B
 *
 * @Composable
 * fun App() {
 *     RegisterNavigation(startNavDestination = Home) {
 *         navGraph {
 *             section<Home, Movies> { // startDestination = Movies
 *                 screen<Movies> { MoviesScreen() }
 *                 screen<Series> { SeriesScreen() }
 *             }
 *
 *             // if you want to combine it with a single screen:
 *             screen<Settings> {
 *                  SettingsScreen()
 *             }
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun RegisterNavigation(
    startNavDestination: NavDestination,
    modifier: Modifier = Modifier,
    content: NavGraphBuilder.() -> TypedGraph
) {
    val navController = rememberNavController()
    val mutableComposeNavigation = rememberMutableComposeNavigation(
        navController = navController,
        startDestination = startNavDestination
    )

    CompositionLocalProvider(LocalNavigator provides mutableComposeNavigation) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = startNavDestination
        ) {
            install(content())
        }
    }
}
