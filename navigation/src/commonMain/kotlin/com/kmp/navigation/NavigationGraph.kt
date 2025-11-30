package com.kmp.navigation

import androidx.compose.runtime.Composable
import com.kmp.navigation.GlobalNavigation.controller
import com.kmp.navigation.GlobalNavigation.navigation
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

    private val sectionRoots =
        mutableMapOf<KClass<out NavSection>, NavDestination>()

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
     *         section<HomeSection>(root = MovieScreenDestination) {
     *             screen<MovieScreenDestination> { MovieScreen() }
     *             screen<SeriesScreenDestination> { SeriesScreen() }
     *         }
     *
     *         section<SettingsSection>(root = SettingsScreenDestination) {
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
        sectionRoots.clear()

        val dsl = RegisterNavigationBuilder(
            registerScreen = { key, screen ->
                if (screens.put(key, screen) != null) {
                    error("Destination $key is already registered.")
                }
            },
            registerDestinationSection = { dest, section ->
                destinationSections[dest] = section
            },
            registerSectionRoot = { section, root ->
                sectionRoots[section] = root
            }
        )
        dsl.builder()

        configured = true

        // Inform the NavigationController about section mapping & roots
        controller.configureSections(
            destinationToSection = destinationSections.toMap(),
            sectionRoots = sectionRoots.toMap()
        )

        // Set initial destination once
        if (controller.state.value.currentDestination == null) {
            navigation.navigateTo(startDestination) {
                clearStack()
            }
        }
    }

    internal fun findScreen(
        destination: NavDestination
    ): (@Composable (NavDestination) -> Unit)? = screens[destination::class]
}
