package com.kmp.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

/**
 * DSL builder for registering destinations and tab groups.
 *
 * Every destination must be registered before it can be navigated to.
 * Destinations are registered as either [content], [screen], or [tabs].
 *
 * - [content] – respects parent bounds, lands in BackStack
 * - [screen] – fullscreen, breaks out of parent bounds, lands in BackStack
 * - [tabs] – does NOT land in BackStack, last active destination per group is remembered
 *
 * Animations can be defined per destination via [enterTransition] and [exitTransition].
 * If not specified, default animations are used.
 *
 * ```kotlin
 * registerNavigation(startDestination = HomeDestination) {
 *
 *     content<HomeDestination> { HomeContent() }
 *     content<SettingsDestination> { SettingsContent() }
 *     content<PopularMovieDestination>(
 *         enterTransition = { slideInVertically { it } + fadeIn() },
 *         exitTransition = { slideOutVertically { -it } + fadeOut() }
 *     ) { PopularMovieListScreen() }
 *
 *     screen<DetailDestination>(
 *         enterTransition = { slideInHorizontally { it } + fadeIn() },
 *         exitTransition = { slideOutHorizontally { -it } + fadeOut() }
 *     ) { dest -> DetailScreen(dest.id) }
 *
 *     tabs<HomeTabs>(
 *         startDestination = MovieDestination,
 *         MovieDestination, SeriesDestination
 *     )
 *     tabs<BottomBarTabs>(
 *         startDestination = HomeDestination,
 *         HomeDestination, SettingsDestination
 *     )
 * }
 * ```
 */
@NavigationDsl
class RegisterNavigationBuilder {

    internal val destinationsWithDat =
        mutableMapOf<KClass<out NavDestination>, NavScreenData>()

    val navTabsWithData =
        mutableMapOf<KClass<out NavTabs>, NavTabsData>()

    val navDestWithTabs =
        mutableMapOf<KClass<out NavDestination>, KClass<out NavTabs>>()

    /**
     * Register a content destination.
     *
     * Content destinations respect parent bounds and land in the BackStack.
     * Use this for destinations that are displayed within an existing layout
     * such as a Scaffold or a Column.
     *
     * Optionally define custom [enterTransition] and [exitTransition] animations.
     * If not specified, default slide + fade animations are used.
     *
     * ```kotlin
     * content<HomeDestination> { HomeContent() }
     *
     * content<PopularMovieDestination>(
     *     enterTransition = { slideInVertically { it } + fadeIn() },
     *     exitTransition = { slideOutVertically { -it } + fadeOut() }
     * ) { PopularMovieListScreen() }
     * ```
     */
    inline fun <reified D : NavDestination> content(
        noinline enterTransition: (AnimatedContentTransitionScope<NavDestination>.() -> EnterTransition)? = null,
        noinline exitTransition: (AnimatedContentTransitionScope<NavDestination>.() -> ExitTransition)? = null,
        noinline composable: @Composable (D) -> Unit
    ) {
        registerDestination(D::class, NavDestinationType.Content, enterTransition, exitTransition, composable)
    }

    /**
     * Register a screen destination.
     *
     * Screen destinations are fullscreen and break out of parent bounds.
     * They land in the BackStack and can be navigated back from.
     * Use this for destinations that should take over the entire screen
     * such as a detail screen or an authentication flow.
     *
     * Optionally define custom [enterTransition] and [exitTransition] animations.
     * If not specified, default slide + fade animations are used.
     *
     * ```kotlin
     * screen<DetailDestination> { dest -> DetailScreen(dest.id) }
     *
     * screen<DetailDestination>(
     *     enterTransition = { slideInHorizontally { it } + fadeIn() },
     *     exitTransition = { slideOutHorizontally { -it } + fadeOut() }
     * ) { dest -> DetailScreen(dest.id) }
     * ```
     */
    inline fun <reified D : NavDestination> screen(
        noinline enterTransition: (AnimatedContentTransitionScope<NavDestination>.() -> EnterTransition)? = null,
        noinline exitTransition: (AnimatedContentTransitionScope<NavDestination>.() -> ExitTransition)? = null,
        noinline composable: @Composable (D) -> Unit
    ) {
        registerDestination(D::class, NavDestinationType.Screen, enterTransition, exitTransition, composable)
    }

    /**
     * Register a tabs group.
     *
     * Tab destinations do NOT land in the BackStack.
     * Switching between tabs replaces the current tab destination.
     * The last active destination per group is remembered and restored
     * when switching back to a previously visited tab.
     *
     * [startDestination] defines which destination is shown first.
     * It must be part of the [destinations] list.
     *
     * ```kotlin
     * tabs<HomeTabs>(
     *     startDestination = MovieDestination,
     *     MovieDestination, SeriesDestination
     * )
     * ```
     */
    inline fun <reified G : NavTabs> tabs(
        startDestination: NavDestination,
        vararg destinations: NavDestination
    ) {
        val groupClass = G::class
        val navDestinationList = destinations.toList()

        if (navDestinationList.isEmpty()) {
            Logger.w("RegisterNavigationBuilder") {
                "tabs<${groupClass.simpleName}>: no destinations provided – skipping."
            }
            return
        }

        if (!navDestinationList.any { it::class == startDestination::class }) {
            Logger.w("RegisterNavigationBuilder") {
                "tabs<${groupClass.simpleName}>: startDestination ${startDestination::class.simpleName} " +
                        "is not in destinations list – using first destination as fallback."
            }
        }

        val resolvedStart = navDestinationList.firstOrNull { it::class == startDestination::class }
            ?: navDestinationList.first()

        navTabsWithData[groupClass] = NavTabsData(
            tabsClass = groupClass,
            startDestination = resolvedStart,
            destinations = navDestinationList
        )

        // Register reverse lookup: destination → group
        navDestinationList.forEach { dest ->
            navDestWithTabs[dest::class] = groupClass
        }

        Logger.i("RegisterNavigationBuilder") {
            "tabs<${groupClass.simpleName}> registered with ${navDestinationList.size} destinations, " +
                    "start: ${resolvedStart::class.simpleName}"
        }
    }

    /**
     * Internal registration function used by [content] and [screen].
     *
     * Registers a destination with its composable content, type, and optional animations.
     * Skips registration if the destination is already registered.
     */
    @PublishedApi
    internal fun <D : NavDestination> registerDestination(
        klass: KClass<D>,
        type: NavDestinationType,
        enterTransition: (AnimatedContentTransitionScope<NavDestination>.() -> EnterTransition)?,
        exitTransition: (AnimatedContentTransitionScope<NavDestination>.() -> ExitTransition)?,
        composable: @Composable (D) -> Unit
    ) {
        if (destinationsWithDat.containsKey(klass)) {
            Logger.w("RegisterNavigationBuilder") {
                "${klass.simpleName} already registered – skipping."
            }
            return
        }

        destinationsWithDat[klass] = NavScreenData(
            content = { dest ->
                @Suppress("UNCHECKED_CAST")
                if (klass.isInstance(dest)) {
                    composable(dest as D)
                } else {
                    Logger.w("RegisterNavigationBuilder") {
                        "Type mismatch for ${klass.simpleName} – skipping render."
                    }
                }
            },
            type = type,
            enterTransition = enterTransition,
            exitTransition = exitTransition
        )

        Logger.i("RegisterNavigationBuilder") {
            "${type.name} <${klass.simpleName}> registered."
        }
    }
}