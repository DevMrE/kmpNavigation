package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import org.koin.mp.KoinPlatform.getKoin
import com.kmp.navigation.compose.RegisterNavigation
import org.koin.compose.koinInject

val LocalNavigation = staticCompositionLocalOf<Navigation> {
    error("No Navigation provided. Make sure you are inside RegisterNavigation.")
}

@Composable
internal fun provideNavigationInstance(): Navigation {
    return runCatching {
        koinInject<Navigation>()
    }.getOrElse {
        remember { NavigationFactory.create() }
    }
}

/**
 * Returns the last [Navigation] from an [RegisterNavigation]
 */
@Composable
fun rememberNavigation(): Navigation = LocalNavigation.current