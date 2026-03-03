package com.kmp.navigation.di

import com.kmp.navigation.GlobalNavigation
import com.kmp.navigation.Navigation
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
    single<Navigation> { GlobalNavigation.navigation }
}