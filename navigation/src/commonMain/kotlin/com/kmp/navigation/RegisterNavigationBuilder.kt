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
    @PublishedApi internal val registerScreen: (
        KClass<out NavDestination>,
        @Composable (NavDestination) -> Unit
    ) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, NavSection) -> Unit,
    @PublishedApi internal val registerSectionRoot: (NavSection, NavDestination) -> Unit,
    @PublishedApi internal val registerSectionParent: (NavSection, NavSection?) -> Unit,
) {

    /**
     * Top-level section (parent = null)
     */
    inline fun <reified S : NavSection> section(
        section: S,
        root: NavDestination,
        noinline builder: SectionBuilder<S>.() -> Unit
    ) {
        registerSectionParent(section, null)
        registerSectionRoot(section, root)

        val sectionBuilder = SectionBuilder(
            section = section,
            registerScreen = registerScreen,
            registerDestinationSection = registerDestinationSection,
            registerSectionRoot = registerSectionRoot,
            registerSectionParent = registerSectionParent
        )
        sectionBuilder.builder()
    }
}

@NavigationDsl
class SectionBuilder<S : NavSection> @PublishedApi internal constructor(
    @PublishedApi internal val section: S,
    @PublishedApi internal val registerScreen: (
        KClass<out NavDestination>,
        @Composable (NavDestination) -> Unit
    ) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, NavSection) -> Unit,
    @PublishedApi internal val registerSectionRoot: (NavSection, NavDestination) -> Unit,
    @PublishedApi internal val registerSectionParent: (NavSection, NavSection?) -> Unit,
) {

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

    /**
     * Nested section (parent = this.section)
     */
    inline fun <reified C : NavSection> section(
        section: C,
        root: NavDestination,
        noinline builder: SectionBuilder<C>.() -> Unit
    ) {
        registerSectionParent(section, this.section)
        registerSectionRoot(section, root)

        val nestedBuilder = SectionBuilder(
            section = section,
            registerScreen = registerScreen,
            registerDestinationSection = registerDestinationSection,
            registerSectionRoot = registerSectionRoot,
            registerSectionParent = registerSectionParent
        )
        nestedBuilder.builder()
    }
}
