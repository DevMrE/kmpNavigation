package com.kmp.navigation

import kotlin.reflect.KClass

/**
 * Platform-agnostic navigation API.
 */
interface Navigation {

    fun <D : NavDestination> navigateTo(
        navDestination: D,
        options: NavOptions.() -> Unit = {}
    )

    /**
     * Switch to the given [NavSection].
     *
     * The implementation will:
     * 1. Look up the last visited destination of that section, if any.
     * 2. Otherwise, fall back to a sensible default (e.g. a configured root).
     * 3. Push that destination onto the global back stack.
     */
    fun <S : NavSection> switchTo(section: KClass<S>)

    fun navigateUp()

    fun <D : NavDestination> popBackTo(
        navDestination: D?,
        inclusive: Boolean = false
    )
}

/**
 * Reified helper to switch by section type.
 *
 * ```kotlin
 * navigation.switchTo<HomeSection>()
 * navigation.switchTo<AuthSection>()
 * ```
 */
inline fun <reified S : NavSection> Navigation.switchTo() {
    switchTo(S::class)
}
