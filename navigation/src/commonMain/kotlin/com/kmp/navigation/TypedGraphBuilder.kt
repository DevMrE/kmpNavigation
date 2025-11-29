package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.kmp.navigation.compose.HandleComposeNavigation
import kotlin.reflect.KClass

/**
 * Type-safe navigation graph builder for Jetpack Compose Navigation (typed routes).
 *
 * You declare your graph using your own [NavDestination] types and this DSL wires
 * them to Navigation-Compose under the hood.
 */
class TypedGraph internal constructor(
     val installBlock: NavGraphBuilder.() -> Unit
)

/**
 * Builder that collects type-safe installers for Navigation-Compose.
 */
class TypedGraphBuilder(
     val currentRoot: KClass<out NavDestination>? = null
) {

    val installers = mutableListOf<NavGraphBuilder.() -> Unit>()

    /**
     * Register a typed screen destination.
     *
     * The [content] composable receives a fully typed [NavDest] instance parsed
     * from the back-stack entry via `toRoute<NavDest>()`.
     *
     * Every time the composable is entered, the navigation layer is notified
     * via [HandleComposeNavigation.onDestinationComposed] so system back
     * gestures and programmatic navigation stay in sync.
     */
    inline fun <reified NavDest : NavDestination> screen(
        noinline content: @Composable (NavDest) -> Unit
    ) {
        // Track which logical "root section" this destination belongs to.
        TypedDestinationRegistry.registerScreen(
            destClass = NavDest::class,
            rootClass = currentRoot
        )

        installers += {
            composable<NavDest> { entry ->
                val dest = entry.toRoute<NavDest>()
                HandleComposeNavigation.onDestinationComposed(dest)
                content(dest)
            }
        }
    }

    /**
     * Declare a nested navigation section.
     *
     * [ParentNavDest] is the typed route representing the section root
     * (for example `Home`), [ChildStartNavDest] is the start destination
     * inside that section.
     */
    inline fun <reified ParentNavDest : NavDestination, reified ChildStartNavDest : NavDestination> section(
        noinline block: TypedGraphBuilder.() -> Unit
    ) {
        // Mark parent as root for all children in this section.
        TypedDestinationRegistry.registerSectionRoot(ParentNavDest::class)

        val childBuilder = TypedGraphBuilder(currentRoot = ParentNavDest::class).apply(block)
        val childGraph = childBuilder.build()

        installers += {
            navigation<ParentNavDest>(startDestination = ChildStartNavDest::class) {
                childGraph.installBlock(this)
            }
        }
    }

    fun build(): TypedGraph =
        TypedGraph { installers.forEach { it(this) } }
}

/**
 * Entry point for building a [TypedGraph].
 */
fun navGraph(block: TypedGraphBuilder.() -> Unit): TypedGraph =
    TypedGraphBuilder().apply(block).build()

/**
 * Installs a [TypedGraph] into the current [NavGraphBuilder].
 */
fun NavGraphBuilder.install(graph: TypedGraph) {
    graph.installBlock(this)
}
