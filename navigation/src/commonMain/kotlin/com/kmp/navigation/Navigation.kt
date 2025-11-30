package com.kmp.navigation

import kotlin.reflect.KClass

/**
 * Platform-agnostic navigation API used by both ViewModels and Compose.
 *
 * ```kotlin
 * class LoginViewModel(
 *     private val navigation: Navigation
 * ) : ViewModel() {
 *
 *     fun onLoginSuccess() {
 *         navigation.navigateTo(HomeScreenDestination) {
 *             clearStack()
 *         }
 *     }
 *
 *     fun onSettingsClicked() {
 *         navigation.switchTo<SettingsSection>()
 *     }
 * }
 * ```
 */
interface Navigation {

    /**
     * Navigate to the given [navDestination] and push it on the global back stack.
     *
     * ```kotlin
     * navigation.navigateTo(MovieScreenDestination)
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
     * The implementation will:
     * * Look up the last visited destination of that section, if any.
     * * If none exists yet -> do nothing.
     * * Otherwise push that destination as a new entry on the global back stack.
     *
     * ```kotlin
     * navigation.switchTo<HomeSection>()
     * navigation.switchTo<SettingsSection>()
     * ```
     */
    fun <S : NavSection> switchTo(section: KClass<S>)

    /**
     * Pop a single entry from the global back stack.
     *
     * If there is only one entry left, this is a no-op.
     *
     * ```kotlin
     * navigation.navigateUp()
     * ```
     */
    fun navigateUp()

    /**
     * Pop entries from the global back stack until [navDestination] is reached.
     *
     * If [navDestination] is null, this behaves like [navigateUp].
     */
    fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean = false
    )
}

/**
 * Reified helper to switch to a section by type.
 *
 * ```kotlin
 * navigation.switchTo<HomeSection>()
 * navigation.switchTo<AuthSection>()
 * ```
 */
inline fun <reified S : NavSection> Navigation.switchTo() {
    switchTo(S::class)
}
