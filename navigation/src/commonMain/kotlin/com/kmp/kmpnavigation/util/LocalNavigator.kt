package com.kmp.kmpnavigation.util

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that provides the current `Navigation` instance to Composables.
 *
 * Provided by: `RegisterNavigation`, which installs a `NavHost` and supplies
 * a `Navigation` implementation to the composition tree.
 *
 * Access pattern:
 * ```kotlin
 * @Composable
 * fun MyComposable() {
 *     val navigation = LocalNavigator.current
 *     // use navigation with a ViewModel context receiver
 * }
 * ```
 */
internal val LocalNavigator = staticCompositionLocalOf<Navigation> {
    error("Navigator not provided")
}