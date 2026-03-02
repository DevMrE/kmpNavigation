package com.kmp.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Top-level DSL builder used by [registerNavigation].
 *
 * Supports nested sections and registers screens within their section scope.
 *
 * ```kotlin
 * registerNavigation(startDestination = AppRootDestination) {
 *
 *     section<AppRootSection>(root = AppRootDestination) {
 *
 *         section<HomeSection>(root = MovieScreenDestination) {
 *             screen<MovieScreenDestination> { MovieScreen() }
 *             screen<SeriesScreenDestination> { SeriesScreen() }
 *         }
 *
 *         section<SettingsSection>(root = SettingsScreenDestination) {
 *             screen<SettingsScreenDestination> { SettingsScreen() }
 *         }
 *     }
 *
 *     section<DetailSection>(root = DetailScreenDestination(id = ""), overlay = true) {
 *         screen<DetailScreenDestination> { detail -> DetailScreen(detail.id) }
 *     }
 * }
 * ```
 */
@NavigationDsl
class RegisterNavigationBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, NavSection) -> Unit,
    @PublishedApi internal val registerSectionRoot: (NavSection, NavDestination, NavSection?, Boolean) -> Unit,
    @PublishedApi internal val currentSection: NavSection? = null
) {

    /**
     * Declare a navigation section, optionally nested inside the current section.
     *
     * @param section The singleton instance of the section.
     * @param root The root destination shown when entering this section for the first time.
     * @param overlay If true (default), this section renders as an overlay on top of its
     * parent when navigated to. If false, it replaces the parent completely.
     *
     * ```kotlin
     * section<HomeSection>(root = MovieScreenDestination) {
     *     screen<MovieScreenDestination> { MovieScreen() }
     *     screen<SeriesScreenDestination> { SeriesScreen() }
     * }
     * ```
     */
    inline fun <reified S : NavSection> section(
        section: S,
        root: NavDestination,
        overlay: Boolean = true,
        noinline builder: RegisterNavigationBuilder.() -> Unit
    ) {
        registerSectionRoot(section, root, currentSection, overlay)

        RegisterNavigationBuilder(
            registerScreen = registerScreen,
            registerDestinationSection = registerDestinationSection,
            registerSectionRoot = registerSectionRoot,
            currentSection = section
        ).builder()
    }

    /**
     * Register a composable screen for destination type [D].
     *
     * Must be called inside a [section] block.
     *
     * ```kotlin
     * screen<MovieScreenDestination> { destination ->
     *     MovieScreen(destination.id)
     * }
     * ```
     */
    inline fun <reified D : NavDestination> screen(
        noinline content: @Composable (D) -> Unit
    ) {
        val section = currentSection ?: error("screen<${D::class.simpleName}> must be called inside a section { } block.")

        registerDestinationSection(D::class, section)
        registerScreen(D::class) { dest ->
            @Suppress("UNCHECKED_CAST")
            content(dest as D)
        }
    }
}