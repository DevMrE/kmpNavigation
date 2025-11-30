package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Section
import kotlin.reflect.KClass

/**
 * DSL marker to avoid mixing navigation builders accidentally.
 */
@DslMarker
annotation class NavigationDsl

/**
 * Top-level DSL builder used by [RegisterNavigation].
 *
 * It lets you declare type-safe "sections" (graphs) and screens:
 *
 * ```kotlin
 * RegisterNavigation(
 *     startDestination = HomeScreenDestination,
 *     builder = {
 *         section<HomeSection, HomeScreenDestination> {
 *             screen<HomeScreenDestination> { HomeScreen() }
 *             screen<SettingsScreenDestination> { SettingsScreen() }
 *         }
 *
 *         section<AuthSection, LoginDestination> {
 *             screen<LoginDestination> { LoginScreen() }
 *             screen<RegisterDestination> { RegisterScreen() }
 *         }
 *     }
 * ) {
 *     NavigationHost()
 * }
 * ```
 */
@NavigationDsl
class RegisterNavigationBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit
) {

    /**
     * Declare a new navigation section.
     *
     * @param S Section type implementing [Section].
     * @param Root Root destination of this section. This is a compile-time hint only.
     *
     * ```kotlin
     * section<HomeSection, HomeScreenDestination> {
     *     screen<HomeScreenDestination> { HomeScreen() }
     *     screen<SettingsScreenDestination> { SettingsScreen() }
     * }
     * ```
     */
    inline fun <reified S : Section, reified Root : NavDestination> section(
        noinline builder: SectionBuilder<S, Root>.() -> Unit
    ) {
        val sectionBuilder = SectionBuilder(
            sectionKey = S::class,
            rootKey = Root::class,
            registerScreen = registerScreen
        )
        sectionBuilder.builder()
    }
}

/**
 * Builder that registers all screens belonging to a section.
 *
 * The section itself ([S]) and the root destination ([Root]) are used only for type safety
 * and documentation. Routing is still based on concrete [NavDestination] types.
 */
@NavigationDsl
class SectionBuilder<S : Section, Root : NavDestination> @PublishedApi internal constructor(
    @PublishedApi internal val sectionKey: KClass<S>,
    @PublishedApi internal val rootKey: KClass<Root>,
    @PublishedApi internal val registerScreen: (KClass<out NavDestination>, @Composable (NavDestination) -> Unit) -> Unit
) {

    /**
     * Register a screen Composable for the given [NavDestination] type [D].
     *
     * ```kotlin
     * section<HomeSection, HomeScreenDestination> {
     *     screen<HomeScreenDestination> { HomeScreen() }
     *     screen<DetailsScreenDestination> { destination ->
     *         DetailsScreen(id = destination.id)
     *     }
     * }
     * ```
     */
    inline fun <reified D : NavDestination> screen(
        noinline content: @Composable (D) -> Unit
    ) {
        val destKey = D::class
        registerScreen(destKey) { dest ->
            @Suppress("UNCHECKED_CAST")
            content(dest as D)
        }
    }
}
