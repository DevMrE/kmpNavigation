package com.kmp.navigation

import androidx.compose.runtime.Composable
import com.kmp.navigation.compose.RegisterNavigationBuilder
import kotlin.reflect.KClass

/**
 * Global registry mapping [NavDestination] types to their composable content.
 *
 * It is configured once via [configureNavigationGraph] / [registerNavigation].
 */
object NavigationGraph {

    private val screens =
        mutableMapOf<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>()
    private var configured: Boolean = false

    /**
     * Returns whether the graph has been configured already.
     */
    fun isConfigured(): Boolean = configured

    /**
     * Configure the navigation graph using the DSL in [RegisterNavigationBuilder].
     *
     * This function is not composable and can be called from any initialization
     * code (e.g. in your Application, main entry point, or before showing the UI).
     *
     * ```kotlin
     * configureNavigationGraph(
     *     startDestination = HomeScreenDestination
     * ) {
     *     section<HomeSection, MovieScreenDestination> {
     *         screen<MovieScreenDestination> { MovieScreen() }
     *         screen<SeriesScreenDestination> { SeriesScreen() }
     *     }
     *     section<AuthSection, LoginDestination> {
     *         screen<LoginDestination> { LoginScreen() }
     *         screen<RegisterDestination> { RegisterScreen() }
     *     }
     * }
     * ```
     */
    fun configureNavigationGraph(
        startDestination: NavDestination,
        builder: RegisterNavigationBuilder.() -> Unit
    ) {
        screens.clear()

        val navBuilder = RegisterNavigationBuilder { key, content ->
            if (screens.put(key, content) != null) {
                error("Destination $key is already registered. Did you register it twice?")
            }
        }
        navBuilder.builder()

        configured = true

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
