package com.kmp.navigation.di

import com.kmp.navigation.compose.ComposeNavigation
import com.kmp.navigation.compose.HandleNavigation
import com.kmp.navigation.compose.MutableComposeNavigation
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
