package com.kmp.navigation

interface Navigation {

    /**
     * Navigate to the given [navDestination] and push it onto the back stack.
     *
     * ```kotlin
     * navigation.navigateTo(MovieContentListDestination)
     * navigation.navigateTo(DetailScreenDestination(id = 42)) {
     *     singleTop = true
     * }
     * ```
     */
    fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit = {}
    )

    /**
     * Switch to the given [section].
     *
     * Does NOT push onto the back stack – the user cannot navigate back
     * to the previous section with [navigateUp].
     *
     * The last visited destination of [section] is restored. If the section
     * has never been visited, the configured root destination is used.
     *
     * ```kotlin
     * navigation.switchTo(HomeSection)
     * navigation.switchTo(SettingsSection)
     * ```
     */
    fun switchTo(section: NavSection)

    /**
     * Switch to the given [destination] directly, without pushing onto the back stack.
     *
     * Unlike [navigateTo], this replaces the current entry in-place.
     * The user cannot navigate back to the previous screen with [navigateUp].
     *
     * Use this for tab switching where the destination is not wrapped in its own section.
     *
     * ```kotlin
     * navigation.switchTo(MovieScreenDestination)
     * navigation.switchTo(SeriesScreenDestination)
     * ```
     */
    fun <D : NavDestination> switchTo(destination: D)

    /**
     * Pop a single entry from the back stack.
     *
     * If there is only one entry left, this is a no-op.
     *
     * ```kotlin
     * navigation.navigateUp()
     * ```
     */
    fun navigateUp()

    /**
     * Pop entries from the back stack until [navDestination] is reached.
     *
     * If [navDestination] is null, this behaves like [navigateUp].
     *
     * ```kotlin
     * navigation.popBackTo(HomeScreenDestination, inclusive = false)
     * ```
     */
    fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean = false
    )
}