package com.kmp.navigation

import androidx.navigation3.runtime.NavKey
import com.kmp.movieapp.core.util.navigation.Navigator
import org.koin.core.module.Module

object NavigationFactory {
    fun create(): Navigation = GlobalNavigation.navigation

    @PublishedApi
    internal fun controller(): NavigationController = GlobalNavigation.controller
}

interface NavigatorFactory<T : NavKey> {
    fun create(start: T): Navigator<T>
}

inline fun <reified T : NavKey> Module.registerNavigatorFactory() {

    factory<NavigatorFactory<T>> {
        DefaultNavigatorFactory()
    }
}