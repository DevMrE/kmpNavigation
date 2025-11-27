package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import org.koin.mp.KoinPlatform.getKoin

@Composable
internal fun rememberMutableComposeNavigation(
    navController: NavHostController
): MutableComposeNavigation {
    val navigation = remember { getKoin().get<MutableComposeNavigation>() }

    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose { navigation.detach() }
    }

    return navigation
}