package com.kmp.navigation

/**
 * Type-safe navigation graph builder for Jetpack Compose Navigation (typed routes).
 *
 * Use this DSL to declare your graph with strongly typed [NavDestination] routes.
 */

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlin.reflect.KClass

class TypedGraph internal constructor(
    internal val installBlock: NavGraphBuilder.() -> Unit
)

/**
 * Builder that collects type-safe installers for Navigation-Compose.
 *
 * You typically write:
 *
 * ```kotlin
 * val appGraph = navGraph {
 *     screen<Home> { HomeScreen() }
 *
 *     section<Home, Movies> {
 *         screen<Movies> { MoviesScreen() }
 *         screen<Series> { SeriesScreen() }
 *     }
 * }
 * ```
 */
class TypedGraphBuilder @PublishedApi internal constructor(
    @PublishedApi internal val currentRoot: KClass<out NavDestination>? = null
) {

    @PublishedApi
    internal val installers = mutableListOf<NavGraphBuilder.() -> Unit>()

    /**
     * Register a typed screen destination.
     *
     * The [content] composable receives a fully typed instance parsed from
     * the NavBackStackEntry via `toRoute<NavDest>()`.
     */
    inline fun <reified NavDest : NavDestination> screen(
        noinline content: @Composable (NavDest) -> Unit
    ) {
        // Track route <-> type + root mapping
        TypedDestinationRegistry.registerScreen<NavDest>(rootClass = currentRoot)

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
     * - [ParentNavDest] is the typed route that represents the graph "root"
     * - [ChildStartNavDest] is the typed start destination inside this section
     */
    inline fun <reified ParentNavDest : NavDestination, reified ChildStartNavDest : NavDestination> section(
        noinline block: TypedGraphBuilder.() -> Unit
    ) {
        // Mark parent as root for all children in this section
        TypedDestinationRegistry.registerSectionRoot<ParentNavDest>()

        val childBuilder = TypedGraphBuilder(currentRoot = ParentNavDest::class).apply(block)
        val childGraph = childBuilder.build()

        installers += {
            navigation<ParentNavDest>(startDestination = ChildStartNavDest::class) {
                childGraph.install(this)
            }
        }
    }

    internal fun build(): TypedGraph =
        TypedGraph { installers.forEach { it(this) } }
}

/**
 * Entry point to build a [TypedGraph].
 */
fun navGraph(block: TypedGraphBuilder.() -> Unit): TypedGraph {
    return TypedGraphBuilder().apply(block).build()
}

/**
 * Install a [TypedGraph] into the current [NavGraphBuilder].
 */
fun NavGraphBuilder.install(graph: TypedGraph) {
    graph.installBlock(this)
}
