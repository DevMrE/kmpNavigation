package com.kmp.kmpnavigation.di

import com.kmp.kmpnavigation.compose_interface.ComposeNavigation
import com.kmp.kmpnavigation.compose_interface.HandleNavigation
import com.kmp.kmpnavigation.compose_interface.MutableComposeNavigation
import com.kmp.kmpnavigation.util.DefaultRouteIdProvider
import com.kmp.kmpnavigation.util.Navigation
import com.kmp.kmpnavigation.util.RouteIdProvider
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

val navigationModule = module {
    single<RouteIdProvider> { DefaultRouteIdProvider }
    single<HandleNavigation> { HandleNavigation(get()) }
    single<MutableComposeNavigation> { ComposeNavigation(get()) }
    single<Navigation> { get<MutableComposeNavigation>() }
}
