package com.kmp.navigation

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

/**
 * DSL builder for registering destinations and tab groups.
 *
 * ```kotlin
 * registerNavigation(startDestination = HomeContentDestination) {
 *
 *     content<HomeContentDestination> { HomeContent() }
 *     content<SettingsContentDestination> { SettingsContent() }
 *     content<MovieContentDestination> { MovieContent() }
 *     content<SeriesContentDestination> { SeriesContent() }
 *
 *     screen<DetailScreenDestination> { dest -> DetailScreen(dest.id) }
 *
 *     tabs<HomeTabs>(
 *         startDestination = MovieContentDestination,
 *         MovieContentDestination, SeriesContentDestination
 *     )
 *     tabs<AppRoot>(
 *         startDestination = HomeContentDestination,
 *         HomeContentDestination, SettingsContentDestination
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
     * Respects parent bounds, lands in BackStack.
     *
     * ```kotlin
     * content<HomeContentDestination> { HomeContent() }
     * content<MovieContentDestination> { MovieContent() }
     * ```
     */
    inline fun <reified D : NavDestination> content(
        noinline composable: @Composable (D) -> Unit
    ) {
        registerDestination(D::class, NavDestinationType.Content, composable)
    }

    /**
     * Register a screen destination.
     * Fullscreen – breaks out of parent bounds, lands in BackStack.
     *
     * ```kotlin
     * screen<DetailScreenDestination> { dest -> DetailScreen(dest.id) }
     * ```
     */
    inline fun <reified D : NavDestination> screen(
        noinline composable: @Composable (D) -> Unit
    ) {
        registerDestination(D::class, NavDestinationType.Screen, composable)
    }

    /**
     * Register a tabs group.
     * Switching between tabs does NOT add to the BackStack.
     * The last active destination per group is remembered.
     *
     * ```kotlin
     * tabs<HomeTabs>(
     *     startDestination = MovieContentDestination,
     *     MovieContentDestination, SeriesContentDestination
     * )
     * ```
     */
    inline fun <reified G : NavTabs> tabs(
        startDestination: NavDestination,
        vararg destinations: NavDestination
    ) {
        val groupClass = G::class
        val navDestinationLisst = destinations.toList()

        if (navDestinationLisst.isEmpty()) {
            Logger.w("RegisterNavigationBuilder") {
                "tabs<${groupClass.simpleName}>: no destinations provided – skipping."
            }
            return
        }

        if (!navDestinationLisst.any { it::class == startDestination::class }) {
            Logger.w("RegisterNavigationBuilder") {
                "tabs<${groupClass.simpleName}>: startDestination ${startDestination::class.simpleName} " +
                        "is not in destinations list – using first destination as fallback."
            }
        }

        val resolvedStart = navDestinationLisst.firstOrNull { it::class == startDestination::class }
            ?: navDestinationLisst.first()

        navTabsWithData[groupClass] = NavTabsData(
            tabsClass = groupClass,
            startDestination = resolvedStart,
            destinations = navDestinationLisst
        )

        // Register reverse lookup
        navDestinationLisst.forEach { dest ->
            navDestWithTabs[dest::class] = groupClass
        }

        Logger.i("RegisterNavigationBuilder") {
            "tabs<${groupClass.simpleName}> registered with ${navDestinationLisst.size} destinations, " +
                    "start: ${resolvedStart::class.simpleName}"
        }
    }

    @PublishedApi
    internal fun <D : NavDestination> registerDestination(
        klass: KClass<D>,
        type: NavDestinationType,
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
            type = type
        )

        Logger.i("RegisterNavigationBuilder") {
            "${type.name} <${klass.simpleName}> registered."
        }
    }
}