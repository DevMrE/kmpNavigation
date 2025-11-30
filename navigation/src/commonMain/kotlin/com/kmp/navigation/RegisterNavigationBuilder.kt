package com.kmp.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * DSL marker to avoid mixing nested builders accidentally.
 *
 * The annotation itself is declared once in [NavigationDsl] and reused here.
 */
@NavigationDsl
class RegisterNavigationBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (
        KClass<out NavDestination>,
        @Composable (NavDestination) -> Unit
    ) -> Unit,
    @PublishedApi internal val registerDestinationSection: (
        KClass<out NavDestination>,
        KClass<out NavSection>
    ) -> Unit
) {

    /**
     * Declare a new navigation section.
     *
     * @param S Section type implementing [NavSection].
     * @param Root Root destination type of this section (for documentation / clarity).
     *
     * ```kotlin
     * registerNavigation(
     *     startDestination = MovieScreenDestination
     * ) {
     *     section<HomeSection, MovieScreenDestination> {
     *         screen<MovieScreenDestination> { MovieScreen() }
     *         screen<SeriesScreenDestination> { SeriesScreen() }
     *     }
     *
     *     section<SettingsSection, SettingsScreenDestination> {
     *         screen<SettingsScreenDestination> { SettingsScreen() }
     *     }
     * }
     * ```
     */
    inline fun <reified S : NavSection, reified Root : NavDestination> section(
        noinline builder: SectionBuilder<S, Root>.() -> Unit
    ) {
        val sectionBuilder = SectionBuilder(
            sectionKey = S::class,
            rootKey = Root::class,
            registerScreen = registerScreen,
            registerDestinationSection = registerDestinationSection
        )
        sectionBuilder.builder()
    }
}

/**
 * Builder that registers all screens belonging to a section.
 *
 * ```kotlin
 * section<HomeSection, MovieScreenDestination> {
 *     screen<MovieScreenDestination> { MovieScreen() }
 *     screen<SeriesScreenDestination> { SeriesScreen() }
 * }
 * ```
 */
@NavigationDsl
class SectionBuilder<S : NavSection, Root : NavDestination> @PublishedApi internal constructor(
    @PublishedApi internal val sectionKey: KClass<S>,
    @PublishedApi internal val rootKey: KClass<Root>,
    @PublishedApi internal val registerScreen: (
        KClass<out NavDestination>,
        @Composable (NavDestination) -> Unit
    ) -> Unit,
    @PublishedApi internal val registerDestinationSection: (
        KClass<out NavDestination>,
        KClass<out NavSection>
    ) -> Unit
) {

    /**
     * Register a screen for destination type [D].
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
        registerDestinationSection(destKey, sectionKey)
        registerScreen(destKey) { dest ->
            @Suppress("UNCHECKED_CAST")
            content(dest as D)
        }
    }
}
