package com.devmre.kmpnavigation.di

import com.devmre.kmpnavigation.compose_interface.ComposeNavigation
import com.devmre.kmpnavigation.compose_interface.HandleNavigation
import com.devmre.kmpnavigation.compose_interface.MutableComposeNavigation
import com.devmre.kmpnavigation.DefaultRouteIdProvider
import com.devmre.kmpnavigation.Navigation
import com.devmre.kmpnavigation.RouteIdProvider
import org.koin.dsl.module

val navigationModule = module {
    single<RouteIdProvider> { DefaultRouteIdProvider }
    single<HandleNavigation> { HandleNavigation(get()) }
    single<MutableComposeNavigation> { ComposeNavigation(get()) }
    single<Navigation> { get<MutableComposeNavigation>() }
}
