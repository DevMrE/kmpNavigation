package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.kmp.navigation.NavigationFactory

@Composable
internal fun rememberMutableComposeNavigation(
    navController: NavHostController
): MutableComposeNavigation {
    val navigation = remember {
        NavigationFactory.mutableInstance
            ?: error(
                "NavigationFactory.create() must be called " +
                        "(example via DI: single<Navigation> { NavigationFactory.create() }) " +
                        "before RegisterNavigation is used."
            )
    }

    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose { navigation.detach() }
    }

    return navigation
}