package com.kmp.navigation

/**
 * Type‑safe navigation graph builder for Jetpack Compose Navigation (typed routes).
 *
 * This small DSL lets you declare a navigation graph using strongly typed
 * destinations that implement your shared [NavDestination]
 * interface. Internally it wires those into Compose Navigation via the generic
 * `composable<Dest>()` and `navigation<Parent>()` builders and resolves arguments
 * using `entry.toRoute<Dest>()`.
 *
 * Quick example
 *
 * ```kotlin
 * // 1) Define your typed destinations (Kotlinx Serializable is typical for typed routes)
 * @kotlinx.serialization.Serializable
 * data class Home : NavDestination
 *
 * @kotlinx.serialization.Serializable
 * data class Movies(val category: String) : NavDestination
 *
 * @kotlinx.serialization.Serializable
 * data class MovieDetails(val id: Long) : NavDestination
 *
 * // 2) Build a graph with screens and optional sections (nested graphs)
 * val appGraph = navGraph {
 *     // A simple screen
 *     screen<Home> { dest ->
 *         HomeScreen(
 *             onOpenCategory = { category ->
 *                 // Navigate elsewhere using your navigation layer
 *                 // navigator.navigate(Movies(category))
 *             }
 *         )
 *     }
 *
 *     // A nested graph: Parent is Movies, start is MovieDetails (or another child)
 *     section<Movies, MovieDetails> {
 *         screen<Movies> { dest ->
 *             MoviesScreen(
 *                 category = dest.category,
 *                 onSelect = { id -> /* navigator.navigate(MovieDetails(id)) */ }
 *             )
 *         }
 *         screen<MovieDetails> { dest ->
 *             MovieDetailsScreen(id = dest.id)
 *         }
 *     }
 * }
 *
 * // 3) Install the graph inside NavHost
 * androidx.navigation.compose.NavHost(
 *     navController = navController,
 *     startDestination = Home::class
 * ) {
 *     install(appGraph)
 * }
 * ```
 *
 * Concepts
 * - screen<Dest>: Registers a typed composable destination. The lambda receives `Dest` parsed
 *   from the back stack entry via `toRoute<Dest>()`.
 * - section<Parent, Start>: Creates a nested graph whose route is `Parent`, and whose start
 *   destination is `Start`. Inside the section block you can call `screen` again.
 * - navGraph { ... }: Produces a [TypedGraph] that can later be installed into a [NavGraphBuilder]
 *   via [install].
 */

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute

class TypedGraph internal constructor(
    val install: NavGraphBuilder.() -> Unit
)

/**
 * Builder that collects type‑safe installers for the Navigation.
 *
 * Use [screen] to add leaf destinations and [section] to add nested graphs. When you
 * are done, wrap everything with [navGraph] and finally call [NavGraphBuilder.install]
 * inside your `NavHost` builder.
 *
 * Usage:
 * ```kotlin
 * navGraph {
 *    screen<NavDestination> {
 *        ComposableContent()
 *    }
 *
 *    section<ParentNavDestination, ChildNavDestination> {
 *        screen<ChildNavDestination> {
 *            ComposableContent()
 *        }
 *
 *        screen<OtherChildNavDestination> {
 *            ComposableContent()
 *        }
 *    }
 * }
 *
 * // Or
 * fun TypedGraphBuilder.homeGraph() {
 *      screen<NavDestination> {
 *           ComposableContent()
 *      }
 *
 *      section<ParentNavDestination, ChildNavDestination> {
 *          screen<ChildNavDestination> {
 *              ComposableContent()
 *          }
 *
 *          screen<OtherChildNavDestination> {
 *              ComposableContent()
 *          }
 *      }
 * }
 *```
 * @see [TypedGraphBuilder.screen]
 * @see [TypedGraphBuilder.section]
 */
class TypedGraphBuilder internal constructor() {

    val installers = mutableListOf<NavGraphBuilder.() -> Unit>()

    /**
     * Register a type‑safe screen for the given destination type [NavDest].
     *
     * The [content] composable receives a fully parsed instance of [NavDest]
     * using `toRoute<NavDest>()`, giving you direct access to typed arguments.
     *
     * Usage
     * ```kotlin
     * @kotlinx.serialization.Serializable
     * data object Settings : NavDestination
     *
     * val graph = navGraph {
     *     screen<Settings> { dest ->
     *         SettingsScreen() // use dest if it had arguments
     *     }
     * }
     *
     * ```
     */
    inline fun <reified NavDest : NavDestination> TypedGraphBuilder.screen(
        noinline content: @Composable (NavDest) -> Unit
    ) {
        installers += {
            composable<NavDest> { entry ->
                val dest = entry.toRoute<NavDest>()
                content(dest)
            }
        }
    }

    /**
     * Declare a nested navigation section.
     *
     * - [ParentNavDest] is the typed route representing the parent graph.
     * - [ChildStartNavDest] is the typed route used as the start destination of the section.
     * - [block] can contain further [screen] or [section] declarations.
     *
     * Usage
     * ```kotlin
     * @kotlinx.serialization.Serializable
     * data object Home : NavDestination // parent graph route
     *
     * @kotlinx.serialization.Serializable
     * data object Movies : NavDestination // child A
     *
     * @kotlinx.serialization.Serializable
     * data object Series : NavDestination // child B
     *
     * val graph = navGraph {
     *     section<Home, Movies> { // startDestination = Movies
     *         screen<Movies> { MoviesScreen() }
     *         screen<Series> { SeriesScreen() }
     *     }
     * }
     *
     * ```
     */
    inline fun <reified ParentNavDest : NavDestination, reified ChildStartNavDest : NavDestination> section(
        noinline block: TypedGraphBuilder.() -> Unit
    ) {
        val child = navGraph(block)
        installers += {
            navigation<ParentNavDest>(startDestination = ChildStartNavDest::class) {
                child.install(this)
            }
        }
    }

    internal fun build(): TypedGraph = TypedGraph {
        installers.forEach { it(this) }
    }
}

/**
 * Creates a [TypedGraph] from the provided [block].
 */
fun navGraph(block: TypedGraphBuilder.() -> Unit): TypedGraph {
    return TypedGraphBuilder().apply(block).build()
}

/**
 * Installs a [TypedGraph] into the current [NavGraphBuilder].
 */
fun NavGraphBuilder.install(graph: TypedGraph) {
    graph.install(this)
}
