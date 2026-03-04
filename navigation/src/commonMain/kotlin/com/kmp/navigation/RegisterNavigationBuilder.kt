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

    internal val destinationMap =
        mutableMapOf<KClass<out NavDestination>, NavScreenData>()

     val tabGroupMap =
        mutableMapOf<KClass<out NavGroup>, NavTabsData>()

    val destToGroup =
        mutableMapOf<KClass<out NavDestination>, KClass<out NavGroup>>()

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
    inline fun <reified G : NavGroup> tabs(
        startDestination: NavDestination,
        vararg destinations: NavDestination
    ) {
        val groupClass = G::class
        val destList = destinations.toList()

        if (destList.isEmpty()) {
            Logger.w("RegisterNavigationBuilder") {
                "tabs<${groupClass.simpleName}>: no destinations provided – skipping."
            }
            return
        }

        if (!destList.any { it::class == startDestination::class }) {
            Logger.w("RegisterNavigationBuilder") {
                "tabs<${groupClass.simpleName}>: startDestination ${startDestination::class.simpleName} " +
                        "is not in destinations list – using first destination as fallback."
            }
        }

        val resolvedStart = destList.firstOrNull { it::class == startDestination::class }
            ?: destList.first()

        tabGroupMap[groupClass] = NavTabsData(
            groupClass = groupClass,
            startDestination = resolvedStart,
            destinations = destList
        )

        // Register reverse lookup
        destList.forEach { dest ->
            destToGroup[dest::class] = groupClass
        }

        Logger.i("RegisterNavigationBuilder") {
            "tabs<${groupClass.simpleName}> registered with ${destList.size} destinations, " +
                    "start: ${resolvedStart::class.simpleName}"
        }
    }

    @PublishedApi
    internal fun <D : NavDestination> registerDestination(
        klass: KClass<D>,
        type: NavDestinationType,
        composable: @Composable (D) -> Unit
    ) {
        if (destinationMap.containsKey(klass)) {
            Logger.w("RegisterNavigationBuilder") {
                "${klass.simpleName} already registered – skipping."
            }
            return
        }

        destinationMap[klass] = NavScreenData(
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