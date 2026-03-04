package com.kmp.navigation

/**
 * Platform-agnostic navigation API.
 *
 * Inject via Koin or use [rememberNavigation] in Composables.
 *
 * Behavior is determined by registration:
 * - `screen<D>` → lands in BackStack, navigateUp() possible
 * - `content<D>` → lands in BackStack, respects parent bounds
 * - `tabs<G>` → does NOT land in BackStack, last active destination per group is remembered
 *
 * ```kotlin
 * class HomeViewModel(private val navigation: Navigation) : ViewModel() {
 *     fun onMovieClicked(id: String) = navigation.navigateTo(DetailScreenDestination(id))
 *     fun onSettingsClicked() = navigation.navigateTo(SettingsContentDestination)
 * }
 * ```
 */
interface Navigation {

    /**
     * Navigate to [destination].
     *
     * If [destination] belongs to a `tabs` group → switches tab, no BackStack entry.
     * If [destination] is a `screen` or `content` → adds to BackStack.
     *
     * ```kotlin
     * navigation.navigateTo(DetailScreenDestination("42"))
     * navigation.navigateTo(SettingsContentDestination)
     * ```
     */
    fun navigateTo(destination: NavDestination)

    /**
     * Pop the top entry from the BackStack.
     * No-op if only one entry remains.
     */
    fun navigateUp()

    /**
     * Pop entries until [destination] is reached.
     *
     * @param inclusive If true, also removes [destination] itself.
     */
    fun popBackTo(destination: NavDestination, inclusive: Boolean = false)

    /**
     * Clear the entire BackStack and navigate to [destination].
     */
    fun clearStackAndNavigateTo(destination: NavDestination)
}