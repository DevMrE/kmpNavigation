package com.kmp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.navigation3.scene.Scene
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

@NavigationDsl
class RegisterNavigationBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, NavScreenData) -> Unit,
    @PublishedApi internal val registerDestinationSection: (KClass<out NavDestination>, NavSection) -> Unit,
    @PublishedApi internal val registerSectionRoot: (NavSection, NavDestination, NavDestination?, NavSection?) -> Unit,
    @PublishedApi internal val currentSection: NavSection? = null
) {

    /**
     * Declares a section with an optional default destination.
     *
     * ```kotlin
     * section(HomeScreenSection, HomeScreenDestination, default = MovieScreenDestination) {
     *     screen<HomeScreenDestination> { HomeScreen() }
     *     screen<MovieScreenDestination> { MovieScreen() }
     *     screen<SeriesScreenDestination> { SeriesScreen() }
     * }
     * ```
     */
    inline fun <reified S : NavSection> section(
        section: S,
        root: NavDestination,
        default: NavDestination? = null,
        noinline builder: RegisterNavigationBuilder.() -> Unit
    ) {
        registerSectionRoot(section, root, default, currentSection)
        RegisterNavigationBuilder(
            registerScreen = registerScreen,
            registerDestinationSection = registerDestinationSection,
            registerSectionRoot = registerSectionRoot,
            currentSection = section
        ).builder()
    }

    /**
     * Registers a screen with optional per-destination animations.
     *
     * ```kotlin
     * screen<MovieScreenDestination> { MovieScreen() }
     *
     * screen<DetailNavDestination>(
     *     transitionSpec = {
     *         slideInVertically { it } + fadeIn() togetherWith
     *         slideOutVertically { -it } + fadeOut()
     *     }
     * ) { dest -> DetailScreen(dest.id) }
     * ```
     */
    inline fun <reified D : NavDestination> screen(
        noinline transitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
        noinline popTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.() -> ContentTransform)? = null,
        noinline predictivePopTransitionSpec: (AnimatedContentTransitionScope<Scene<NavDestination>>.(Int) -> ContentTransform)? = null,
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
        registerScreen(
            D::class,
            NavScreenData(
                content = { dest ->
                    if (dest is D) content(dest)
                    else Logger.w("RegisterNavigationBuilder") {
                        "Type mismatch for ${D::class.simpleName} – skipping render."
                    }
                },
                transitionSpec = transitionSpec,
                popTransitionSpec = popTransitionSpec,
                predictivePopTransitionSpec = predictivePopTransitionSpec
            )
        )
    }
}