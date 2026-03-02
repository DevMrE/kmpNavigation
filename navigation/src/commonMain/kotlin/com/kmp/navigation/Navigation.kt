package com.kmp.navigation

interface Navigation {

    /**
     * Navigate to the given [navDestination] and push it onto the back stack.
     *
     * Use this for forward navigation where the user should be able to
     * navigate back with [navigateUp].
     *
     * ```kotlin
     * navigation.navigateTo(MovieContentListDestination)
     *
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
     * This does NOT push onto the back stack – the user cannot navigate
     * back to the previous section with [navigateUp].
     *
     * The last visited destination of [section] is restored. If the section
     * has never been visited, the configured root destination is used.
     *
     * ```kotlin
     * // BottomBar click
     * navigation.switchTo(HomeSection)
     * navigation.switchTo(SettingsSection)
     *
     * // Tab click inside HomeSection
     * navigation.switchTo(SeriesTab)
     * ```
     */
    fun switchTo(section: NavSection)

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