package com.kmp.navigation

import com.kmp.navigation.compose.MutableComposeNavigation
import com.kmp.navigation.compose.NavigationImpl

/**
 * Factory for creating and sharing the Navigation implementation.
 *
 * - Use this from DI:
 *   single<Navigation> { NavigationFactory.create() }
 *
 * - Internally, the Compose side (rememberMutableComposeNavigation) will also
 *   call [create] as a fallback, so there is always exactly ONE shared instance.
 */
object NavigationFactory {

    /**
     * Backing instance used by the Compose layer.
     *
     * Do not mutate this directly from the outside. Always go through [create].
     */
    var mutableInstance: MutableComposeNavigation? = null
        internal set

    /**
     * Returns a shared [Navigation] implementation.
     *
     * If an instance already exists (created earlier by Compose or DI),
     * it is reused. Otherwise a new [NavigationImpl] is created.
     */
    fun create(): Navigation {
        val existing = mutableInstance
        if (existing != null) return existing

        val impl = NavigationImpl()
        mutableInstance = impl
        return impl
    }
}
