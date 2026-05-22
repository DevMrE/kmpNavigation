package com.kmp.navigation.di

import com.kmp.movieapp.core.util.navigation.Navigator
import com.kmp.movieapp.core.util.navigation.Route
import com.kmp.navigation.Navigation
import com.kmp.navigation.NavigationFactory
import com.kmp.navigation.NavigatorImpl
import org.koin.dsl.module

/**
 * Koin module providing the global [Navigation] singleton.
 *
 * ```kotlin
 * startKoin {
 *     modules(navigationModule, appModule)
 * }
 * ```
 */
val navigationModule = module {
    single<Navigation> { NavigationFactory.create() }
}