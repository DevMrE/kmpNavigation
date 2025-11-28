package com.kmp.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun InstallNavigationBackHandler(
    navigation: Navigation,
    modifier: Modifier
): Modifier {
    BackHandler(enabled = true) {
        navigation.navigateUp()
    }
    return modifier
}