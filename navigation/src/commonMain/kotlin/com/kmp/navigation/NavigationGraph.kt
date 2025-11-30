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

    private val sectionRoots =
        mutableMapOf<KClass<out NavSection>, NavDestination>()

    // section type -> position index (used for swipe direction)
    private val sectionIndices =
        mutableMapOf<KClass<out NavSection>, Int>()

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
        sectionIndices.clear()

        var nextSectionIndex = 0

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
                if (!sectionIndices.containsKey(section)) {
                    sectionIndices[section] = nextSectionIndex++
                }
            }
        )
        dsl.builder()

        configured = true

        // Inform the NavigationController about section mapping & roots
        GlobalNavigation.controller.configureSections(
            destinationToSection = destinationSections.toMap(),
            sectionRoots = sectionRoots.toMap()
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

    /**
     * Returns the position index of the section that the given [destination] belongs to,
     * or `null` if the destination is not associated with any section.
     *
     * The index is assigned in declaration order of `section<...>()` inside the DSL.
     */
    internal fun sectionIndexFor(destination: NavDestination): Int? {
        val sectionClass = destinationSections[destination::class] ?: return null
        return sectionIndices[sectionClass]
    }
}
