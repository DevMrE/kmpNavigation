package com.devmre.kmpnavigation.di

import com.devmre.kmpnavigation.compose_interface.ComposeNavigation
import com.devmre.kmpnavigation.compose_interface.HandleNavigation
import com.devmre.kmpnavigation.compose_interface.MutableComposeNavigation
import com.devmre.kmpnavigation.DefaultRouteIdProvider
import com.devmre.kmpnavigation.Navigation
import com.devmre.kmpnavigation.RouteIdProvider
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
