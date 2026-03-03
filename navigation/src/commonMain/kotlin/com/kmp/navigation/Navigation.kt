package com.kmp.navigation

/**
 * Platform-agnostic navigation API.
 *
 * Inject this interface into ViewModels via Koin or use
 * [rememberNavigation] in Composables.
 *
 * ```kotlin
 * class HomeViewModel(private val navigation: Navigation) : ViewModel() {
 *     fun onMovieClicked(id: String) {
 *         navigation.navigateTo(DetailDestination(id))
 *     }
 *     fun onSettingsClicked() {
 *         navigation.switchTo(SettingsSection)
 *     }
 * }
 * ```
 */
interface Navigation {

    /**
     * Push [destination] onto the back stack.
     *
     * ```kotlin
     * navigation.navigateTo(DetailDestination(id = "42"))
     * navigation.navigateTo(ProfileDestination) { singleTop = true }
     * ```
     */
    fun <D : NavDestination> navigateTo(
        destination: D,
        options: NavOptions.() -> Unit = {}
    )

    /**
     * Switch to [section] without pushing onto the back stack.
     *
     * Builds a full shell chain automatically. The last visited destination
     * of [section] is restored. If never visited, the configured root is used.
     *
     * ```kotlin
     * navigation.switchTo(HomeSection)
     * navigation.switchTo(SettingsSection)
     * ```
     */
    fun switchTo(
        section: NavSection,
        transition: NavTransitionSpec? = null
    )

    /**
     * Switch to [destination] without pushing onto the back stack.
     *
     * Use this for tab switching within a section.
     *
     * ```kotlin
     * navigation.switchTo(MovieDestination)
     * navigation.switchTo(SeriesDestination)
     * ```
     */
    fun <D : NavDestination> switchTo(
        destination: D,
        transition: NavTransitionSpec? = null
    )

    /**
     * Pop the top entry from the back stack.
     * No-op if only one entry remains.
     */
    fun navigateUp()

    /**
     * Pop entries until [destination] is reached.
     *
     * @param inclusive If true, also removes [destination] itself.
     */
    fun <D : NavDestination> popBackTo(
        destination: D,
        inclusive: Boolean = false
    )

    /**
     * Pop entries until the root destination of [section] is reached.
     *
     * @param inclusive If true, also removes the section root itself.
     */
    fun popBackTo(
        section: NavSection,
        inclusive: Boolean = false
    )

    /**
     * Clear the entire back stack and navigate to [destination].
     */
    fun <D : NavDestination> clearStackAndNavigate(destination: D)
}