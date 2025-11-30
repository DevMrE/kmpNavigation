package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Navigation
import com.kmp.navigation.TypedGraphBuilder
import com.kmp.navigation.TypedGraph
import com.kmp.navigation.rememberNavigation
import kotlin.reflect.KClass

/**
 * Sets up a typed `RegisterNavigation` and provides the app navigator to the composition.
 *
 * You MUST return a [TypedGraph] from [content] using the typed entry
 * point `navGraph { ... }`. Inside that block register destinations with
 * [com.kmp.navigation.screen] or nested graphs with [TypedGraphBuilder.section].
 *
 * Parameters
 * - `startNavDestination`: initial typed destination instance (your `NavDestination`).
 * - `modifier`: optional modifier passed to `RegisterNavigation`.
 * - `content`: a builder that returns a [TypedGraph] via `navGraph { ... }`.
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
    builder: TypedGraphBuilder.() -> Unit
) {
    val navigation: Navigation = rememberNavigation()
    val impl = navigation as? NavigationImpl
        ?: error("RegisterNavigation funktioniert nur mit der NavigationFactory.create()-Implementierung (NavigationController).")

    val screenMap = remember {
        val map = mutableMapOf<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>()

        val typedBuilder = TypedGraphBuilder(
            registerScreen = { key, content ->
                if (map.put(key, content) != null) {
                    error("Destination $key ist bereits im TypedGraph registriert.")
                }
            })

        typedBuilder.builder()

        map.toMap()
    }

    val state by impl.state.collectAsState()
    val current = state.currentDestination ?: return

    val content = screenMap[current::class]
        ?: error("Kein Screen für Destination ${current::class} registriert. Hast du ihn in RegisterNavigation{} eingetragen?")

    content(current)
}