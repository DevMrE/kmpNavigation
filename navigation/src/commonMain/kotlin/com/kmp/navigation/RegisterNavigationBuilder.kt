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
 *     section(
 *         section = HomeSection,
 *         root = MovieScreenDestination
 *     ) {
 *         screen<MovieScreenDestination> { MovieScreen() }
 *         screen<SeriesScreenDestination> { SeriesScreen() }
 *     }
 *
 *     section(
 *         section = SettingsSection,
 *         root = SettingsScreenDestination
 *     ) {
 *         screen<SettingsScreenDestination> { SettingsScreen() }
 *     }
 * }
 * ```
 */
@NavigationDsl
class RegisterNavigationBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, NavSection) -> Unit,
    @PublishedApi internal val registerSectionRoot: (NavSection, NavDestination) -> Unit
) {

    /**
     * Declare a new navigation section.
     *
     * @param S Section type implementing [NavSection].
     * @param section The concrete singleton instance of the section, e.g. HomeSection.
     * @param root Root destination instance for this section. This is used when switching
     * to the section for the first time or when there is no "last screen" remembered.
     *
     * ```kotlin
     * section(
     *     section = HomeSection,
     *     root = MovieScreenDestination
     * ) {
     *     screen<MovieScreenDestination> { MovieScreen() }
     *     screen<SeriesScreenDestination> { SeriesScreen() }
     * }
     * ```
     */
    inline fun <reified S : NavSection> section(
        section: S,
        root: NavDestination,
        noinline builder: SectionBuilder<S>.() -> Unit
    ) {
        // remember root destination for this section
        registerSectionRoot(section, root)

        val sectionBuilder = SectionBuilder(
            section = section,
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
 * section(
 *     section = HomeSection,
 *     root = MovieScreenDestination
 * ) {
 *     screen<MovieScreenDestination> { MovieScreen() }
 *     screen<SeriesScreenDestination> { SeriesScreen() }
 * }
 * ```
 */
@NavigationDsl
class SectionBuilder<S : NavSection> @PublishedApi internal constructor(
    @PublishedApi internal val section: S,
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, NavSection) -> Unit
) {

    /**
     * Register a screen for destination type [D].
     *
     * ```kotlin
     * section(
     *     section = HomeSection,
     *     root = MovieScreenDestination
     * ) {
     *     screen<MovieScreenDestination> { MovieScreen() }
     *     screen<SeriesScreenDestination> { SeriesScreen() }
     * }
     * ```
     */
    inline fun <reified D : NavDestination> screen(
        noinline content: @Composable (D) -> Unit
    ) {
        val destKey = D::class
        registerDestinationSection(destKey, section)
        registerScreen(destKey) { dest ->
            @Suppress("UNCHECKED_CAST")
            content(dest as D)
        }
    }
}
