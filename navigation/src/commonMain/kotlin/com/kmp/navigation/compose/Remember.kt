package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.kmp.navigation.NavigationFactory
import org.koin.mp.KoinPlatform.getKoin

@Composable
internal fun rememberMutableComposeNavigation(
    navController: NavHostController
): MutableComposeNavigation {
    val navigation = remember {
        NavigationFactory.mutableInstance
            ?: error(
                "NavigationFactory.create() must be called " +
                        "(z.B. via DI: single<Navigation> { NavigationFactory.create() }) " +
                        "bevor RegisterNavigation/rememberMutableComposeNavigation verwendet wird."
            )
    }

    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose { navigation.detach() }
    }

    return navigation
}