package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.kmp.navigation.NavDestination
import kotlinx.coroutines.flow.collectLatest
import org.koin.mp.KoinPlatform.getKoin

@Composable
internal fun rememberMutableComposeNavigation(
    navController: NavHostController,
    startDestination: NavDestination
): MutableComposeNavigation {
    val navigation = remember { getKoin().get<MutableComposeNavigation>() }

    // Initial destination into our HandleComposeNavigation state
    LaunchedEffect(startDestination) {
        HandleComposeNavigation.onBackstackDestinationChanged(startDestination)
    }

    // Mirror every NavController backstack change into HandleComposeNavigation
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            HandleComposeNavigation.onBackstackDestinationChanged(entry.destination.id)
        }
    }

    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose {
            navigation.detach()
            HandleComposeNavigation.onBackstackDestinationChanged(null as NavDestination?)
        }
    }

    return navigation
}