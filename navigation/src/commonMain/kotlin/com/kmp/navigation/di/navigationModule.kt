package com.kmp.navigation.di

import NavigationFactory
import com.kmp.navigation.Navigation
import org.koin.dsl.module

/**
 * Add this navigation module to koin
 *
 * ```kotlin
 *
 * // example
 * startKoin {
 *    modules(navigationModule, otherModule)
 * }
 * ```
 * @see [Navigation]
 */
val navigationModule = module {
    single<Navigation> { NavigationFactory.create() }
}
