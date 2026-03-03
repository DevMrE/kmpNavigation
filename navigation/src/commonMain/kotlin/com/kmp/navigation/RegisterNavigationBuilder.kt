package com.kmp.navigation

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

/**
 * DSL builder for registering sections and screens.
 *
 * ```kotlin
 * registerNavigation(startDestination = MovieDestination) {
 *
 *     section(AppRootSection, AppRootDestination) {
 *         screen<AppRootDestination> { AppRootScreen() }
 *
 *         section(HomeSection, HomeDestination) {
 *             screen<HomeDestination> { HomeScreen() }
 *             screen<MovieDestination> { MovieScreen() }
 *             screen<SeriesDestination> { SeriesScreen() }
 *         }
 *
 *         screen<SettingsDestination> { SettingsScreen() }
 *     }
 *
 *     section(DetailSection, DetailDestination(id = "")) {
 *         screen<DetailDestination> { dest -> DetailScreen(dest.id) }
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
     * Declare a section, optionally nested inside the current section.
     *
     * @param section The singleton section instance.
     * @param root The root destination of this section.
     * @param overlay Reserved for future overlay support.
     */
    inline fun <reified S : NavSection> section(
        section: S,
        root: NavDestination,
        overlay: Boolean = false,
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
     * Register a screen for destination [D] within the current section.
     */
    inline fun <reified D : NavDestination> screen(
        noinline content: @Composable (D) -> Unit
    ) {
        val section = currentSection
        if (section == null) {
            Logger.w("RegisterNavigationBuilder") {
                "screen<${D::class.simpleName}> called outside a section block – skipping."
            }
            return
        }
        registerDestinationSection(D::class, section)
        registerScreen(D::class) { dest ->
            if (dest is D) {
                content(dest)
            } else {
                Logger.w("RegisterNavigationBuilder") {
                    "Type mismatch for ${D::class.simpleName} – skipping render."
                }
            }
        }
    }
}