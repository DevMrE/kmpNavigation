package com.kmp.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * Global registry mapping [NavDestination] types to their composable content
 * and tracking which section each destination belongs to.
 */
object NavigationGraph {

    private val screens =
        mutableMapOf<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>()

    private val destinationSections =
        mutableMapOf<KClass<out NavDestination>, KClass<out NavSection>>()

    private var configured: Boolean = false

    /**
     * Returns whether the graph has been configured already.
     */
    fun isConfigured(): Boolean = configured

    /**
     * Configure the navigation graph and set [startDestination] as the initial entry.
     *
     * This is a regular Kotlin function and can be called from any initialization
     * code (e.g. Application.onCreate, before you render your Compose UI).
     *
     * ```kotlin
     * fun registerAppNavigation() {
     *     registerNavigation(startDestination = MovieScreenDestination) {
     *         section<HomeSection, MovieScreenDestination> {
     *             screen<MovieScreenDestination> { MovieScreen() }
     *             screen<SeriesScreenDestination> { SeriesScreen() }
     *         }
     *
     *         section<SettingsSection, SettingsScreenDestination> {
     *             screen<SettingsScreenDestination> { SettingsScreen() }
     *         }
     *     }
     * }
     * ```
     */
    fun configureNavigationGraph(
        startDestination: NavDestination,
        builder: RegisterNavigationBuilder.() -> Unit
    ) {
        screens.clear()
        destinationSections.clear()

        val dsl = RegisterNavigationBuilder(
            registerScreen = { key, screen ->
                if (screens.put(key, screen) != null) {
                    error("Destination $key is already registered.")
                }
            },
            registerDestinationSection = { dest, section ->
                destinationSections[dest] = section
            }
        )
        dsl.builder()

        configured = true

        // Inform the NavigationController about section mapping
        GlobalNavigation.controller.configureSections(
            destinationToSection = destinationSections.toMap()
        )

        // Set initial destination once
        val controller = GlobalNavigation.controller
        if (controller.state.value.currentDestination == null) {
            GlobalNavigation.navigation.navigateTo(startDestination) {
                clearStack()
            }
        }
    }

    internal fun findScreen(
        destination: NavDestination
    ): (@Composable (NavDestination) -> Unit)? = screens[destination::class]
}
