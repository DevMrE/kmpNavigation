package com.kmp.navigation.di

import com.kmp.navigation.compose_interface.ComposeNavigation
import com.kmp.navigation.compose_interface.HandleNavigation
import com.kmp.navigation.compose_interface.MutableComposeNavigation
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
    single<HandleNavigation> { HandleNavigation }
    single<MutableComposeNavigation> { ComposeNavigation() }
    single<Navigation> { get<MutableComposeNavigation>() }
}
