package com.kmp.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.kmp.navigation.NavDestination
import com.kmp.navigation.Navigation
import com.kmp.navigation.NavigationFactory
import kotlinx.coroutines.flow.collectLatest
import org.koin.mp.KoinPlatform.getKoin

@Composable
internal fun rememberMutableComposeNavigation(
    navController: NavHostController,
    startDestination: NavDestination
): MutableComposeNavigation {
    val navigation = remember {
        NavigationFactory.mutableInstance
            ?: error(
                "NavigationFactory.mutableInstance is null. " +
                        "Make sure you provide Navigation via DI, e.g.: " +
                        "single<Navigation> { NavigationFactory.create() }"
            )
    }

    DisposableEffect(navigation, navController) {
        navigation.attach(navController)
        onDispose { navigation.detach() }
    }

    LaunchedEffect(startDestination) {
        HandleComposeNavigation.onBackstackDestinationChanged(startDestination)
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collectLatest { entry ->
            HandleComposeNavigation.onBackstackDestinationChanged(entry.destination.id)
        }
    }

    return navigation
}