package com.kmp.navigation

import com.kmp.navigation.compose.MutableComposeNavigation
import com.kmp.navigation.compose.NavigationImpl

object NavigationFactory {

    internal var mutableInstance: MutableComposeNavigation? = null
        private set

    /**
     * Creates an instance for the navigation
     */
    fun create(): Navigation {
        val impl = NavigationImpl()
        mutableInstance = impl
        return impl
    }
}