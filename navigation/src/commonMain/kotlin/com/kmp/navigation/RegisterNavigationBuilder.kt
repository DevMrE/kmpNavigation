package com.kmp.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Top-level DSL builder used by [registerNavigation].
 *
 * It lets you declare type-safe sections and screens:
 *
 * ```kotlin
 * registerNavigation(
 *     startDestination = MovieScreenDestination
 * ) {
 *     section<HomeSection>(root = MovieScreenDestination) {
 *         screen<MovieScreenDestination> { MovieScreen() }
 *         screen<SeriesScreenDestination> { SeriesScreen() }
 *     }
 *
 *     section<SettingsSection>(root = SettingsScreenDestination) {
 *         screen<SettingsScreenDestination> { SettingsScreen() }
 *     }
 * }
 * ```
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
    ) -> Unit,
    @PublishedApi internal val registerSectionRoot: (
        KClass<out NavSection>,
        NavDestination
    ) -> Unit
) {

    /**
     * Declare a new navigation section.
     *
     * @param S Section type implementing [NavSection].
     * @param root Root destination instance of this section.
     *
     * ```kotlin
     * section<HomeSection>(root = MovieScreenDestination) {
     *     screen<MovieScreenDestination> { MovieScreen() }
     *     screen<SeriesScreenDestination> { SeriesScreen() }
     * }
     * ```
     */
    inline fun <reified S : NavSection> section(
        root: NavDestination,
        noinline builder: SectionBuilder<S>.() -> Unit
    ) {
        registerSectionRoot(S::class, root)

        val sectionBuilder = SectionBuilder(
            sectionKey = S::class,
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
 * section<HomeSection>(root = MovieScreenDestination) {
 *     screen<MovieScreenDestination> { MovieScreen() }
 *     screen<SeriesScreenDestination> { SeriesScreen() }
 * }
 * ```
 */
@NavigationDsl
class SectionBuilder<S : NavSection> @PublishedApi internal constructor(
    @PublishedApi internal val sectionKey: KClass<S>,
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
     * section<HomeSection>(root = MovieScreenDestination) {
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
