package com.kmp.kmpnavigation.compose_interface

import androidx.navigation.NavHostController
import com.kmp.kmpnavigation.util.Navigation

/**
 * Extension of `Navigation` that can be wired to a `NavHostController`.
 *
 * - `attach(controller)`: called when a `NavHostController` is available.
 * - `detach()`: called when the controller should be released.
 *
 * Used by `RegisterNavigation` via `rememberMutableComposeNavigation()` to
 * provide a Compose-bound navigation implementation to the UI tree.
 */
internal interface MutableComposeNavigation : Navigation {
    fun attach(controller: NavHostController)
    fun detach()
}