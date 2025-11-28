package com.kmp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.kmp.navigation.Navigation

@Composable
internal actual fun InstallNavigationBackHandler(
    navigation: Navigation,
    modifier: Modifier
): Modifier {
    return modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && (event.key == Key.Escape || (event.key == Key.DirectionLeft && (event.isAltPressed || event.isMetaPressed)))) {
            navigation.navigateUp()
            true
        } else {
            false
        }
    }
}