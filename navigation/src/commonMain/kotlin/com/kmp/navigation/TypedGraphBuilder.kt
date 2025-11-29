package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute

/**
 * Wrapper around a set of installers that can be applied to a NavGraphBuilder.
 */
class TypedGraph(
    val install: NavGraphBuilder.() -> Unit
)

/**
 * Type-safe builder for Jetpack Compose Navigation using typed [NavDestination] routes.
 *
 * Example:
 *
 * ```kotlin
 * @kotlinx.serialization.Serializable
 * data object Home : NavDestination
 *
 * @kotlinx.serialization.Serializable
 * data object Movies : NavDestination
 *
 * @kotlinx.serialization.Serializable
 * data object Series : NavDestination
 *
 * fun TypedGraphBuilder.homeGraph() {
 *     section<Home, Movies> {
 *         screen<Movies> { MoviesScreen() }
 *         screen<Series> { SeriesScreen() }
 *     }
 * }
 * ```
 */
class TypedGraphBuilder {

    /**
     * Collected installers that will be applied to a [NavGraphBuilder]
     * when the [TypedGraph] is installed.
     */
    val installers: MutableList<NavGraphBuilder.() -> Unit> = mutableListOf()

    /**
     * Register a type-safe screen for the given [NavDest].
     *
     * The [content] composable receives a strongly typed instance of [NavDest],
     * created from the current back stack entry via `entry.toRoute<NavDest>()`.
     */
    inline fun <reified NavDest : NavDestination> screen(
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
     * - [ParentNavDest] is the typed route representing the section root.
     * - [ChildStartNavDest] is the typed start destination of the section.
     *
     * Inside [block] you can call [screen] (and if you want, even other sections).
     */
    inline fun <reified ParentNavDest : NavDestination, reified ChildStartNavDest : NavDestination> section(
        noinline block: TypedGraphBuilder.() -> Unit
    ) {
        val childBuilder = TypedGraphBuilder().apply(block)
        val childGraph = TypedGraph { childBuilder.installers.forEach { it(this) } }

        installers += {
            navigation<ParentNavDest>(startDestination = ChildStartNavDest::class) {
                childGraph.install(this)
            }
        }
    }

    /**
     * Build a [TypedGraph] from the current installers.
     */
    fun build(): TypedGraph =
        TypedGraph { installers.forEach { it(this) } }
}

/**
 * Entry point to create a [TypedGraph] using the [TypedGraphBuilder] DSL.
 */
fun navGraph(block: TypedGraphBuilder.() -> Unit): TypedGraph {
    val builder = TypedGraphBuilder().apply(block)
    return builder.build()
}

/**
 * Install a [TypedGraph] into the current [NavGraphBuilder].
 */
fun NavGraphBuilder.install(graph: TypedGraph) {
    graph.install(this)
}
