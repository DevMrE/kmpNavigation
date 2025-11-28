package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute

/**
 * Small wrapper around a list of installers that can be applied
 * to a [NavGraphBuilder].
 */
class TypedGraph internal constructor(
    @PublishedApi internal val installers: List<NavGraphBuilder.() -> Unit>
)

/**
 * Builder for type-safe navigation graphs based on [NavDestination].
 *
 * Use [screen] to register leaf destinations and [section] to define
 * nested graphs (parent route + typed start destination).
 */
class TypedGraphBuilder {

    @PublishedApi
    internal val installers = mutableListOf<NavGraphBuilder.() -> Unit>()

    /**
     * Register a type-safe screen for [NavDest].
     *
     * The [content] lambda receives a parsed instance of [NavDest]
     * created via `entry.toRoute<NavDest>()`.
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
     * - [ParentNavDest] is the route of the nested graph.
     * - [ChildStartNavDest] is the typed start destination of that graph.
     * - [block] can register more [screen] or [section] entries.
     */
    inline fun <reified ParentNavDest : NavDestination, reified ChildStartNavDest : NavDestination> section(
        noinline block: TypedGraphBuilder.() -> Unit
    ) {
        val childGraph = navGraph(block)

        installers += {
            navigation<ParentNavDest>(startDestination = ChildStartNavDest::class) {
                // IMPORTANT: use the *extension* install(), not a property
                install(childGraph)
            }
        }
    }

    internal fun build(): TypedGraph = TypedGraph(installers.toList())
}

/**
 * Build a [TypedGraph] from the given [block].
 */
fun navGraph(block: TypedGraphBuilder.() -> Unit): TypedGraph {
    val builder = TypedGraphBuilder()
    builder.block()
    return builder.build()
}

/**
 * Install a [TypedGraph] into this [NavGraphBuilder].
 */
fun NavGraphBuilder.install(graph: TypedGraph) {
    graph.installers.forEach { installer -> installer(this) }
}
