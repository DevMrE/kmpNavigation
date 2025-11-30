package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Section
import com.kmp.navigation.NavigationGraph
import kotlin.reflect.KClass

@DslMarker
annotation class NavigationDsl

/**
 * Top-level DSL builder used by [NavigationGraph.configureNavigationGraph].
 *
 * ```kotlin
 * NavigationGraph.configureNavigationGraph(
 *     startDestination = HomeScreenDestination
 * ) {
 *     section<HomeSection, MovieScreenDestination> {
 *         screen<MovieScreenDestination> { MovieScreen() }
 *         screen<SeriesScreenDestination> { SeriesScreen() }
 *     }
 * }
 * ```
 */
@NavigationDsl
class RegisterNavigationBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit
) {

    /**
     * Declare a new typed section (graph).
     *
     * @param S Section type implementing [Section].
     * @param Root Root destination of this section.
     *
     * ```kotlin
     * section<HomeSection, MovieScreenDestination> {
     *     screen<MovieScreenDestination> { MovieScreen() }
     * }
     * ```
     */
    inline fun <reified S : Section, reified Root : NavDestination> section(
        noinline builder: SectionBuilder<S, Root>.() -> Unit
    ) {
        val sectionBuilder = SectionBuilder(
            sectionKey = S::class,
            rootKey = Root::class,
            registerScreen = registerScreen
        )
        sectionBuilder.builder()
    }
}

/**
 * Builder that registers all screens belonging to a section.
 */
@NavigationDsl
class SectionBuilder<S : Section, Root : NavDestination> @PublishedApi internal constructor(
    @PublishedApi internal val sectionKey: KClass<S>,
    @PublishedApi internal val rootKey: KClass<Root>,
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit
) {

    /**
     * Register a screen for [D].
     *
     * ```kotlin
     * section<HomeSection, MovieScreenDestination> {
     *     screen<MovieScreenDestination> { MovieScreen() }
     *     screen<SeriesScreenDestination> { SeriesScreen() }
     * }
     * ```
     */
    inline fun <reified D : NavDestination> screen(
        noinline content: @Composable (D) -> Unit
    ) {
        val destKey = D::class
        registerScreen(destKey) { dest ->
            @Suppress("UNCHECKED_CAST") content(dest as D)
        }
    }
}
