package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun InstallNavigationBackHandler(
    navigation: Navigation,
    modifier: Modifier = Modifier
): Modifier
