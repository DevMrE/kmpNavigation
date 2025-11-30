package com.kmp.navigation

import androidx.compose.runtime.Composable
import com.kmp.navigation.compose.RegisterNavigationBuilder
import kotlin.reflect.KClass

/**
 * Global registry mapping [NavDestination] types to their composable content
 * and tracking section membership.
 */
object NavigationGraph {

    private val screens =
        mutableMapOf<KClass<out NavDestination>, @Composable (NavDestination) -> Unit>()

    private val destinationSections =
        mutableMapOf<KClass<out NavDestination>, KClass<out NavSection>>()

    private var configured: Boolean = false

    fun isConfigured(): Boolean = configured

    /**
     * Configure the navigation graph and set [startDestination] as the initial entry.
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
            registerSectionRoot = { section, root ->
                // we no longer need root KClass here for reflection
                // but we keep section-root info available if you want to use it later
                // (e.g. for debugging or tooling)
                destinationSections[root] = section
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
