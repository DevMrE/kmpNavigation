package com.kmp.navigation.compose

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import com.kmp.navigation.NavOptions
import com.kmp.navigation.NavDestination

/**
 * Navigate up in the navigation stack.
 */
fun Modifier.navigateUp(): Modifier = this.then(
    Modifier.clickable {
        HandleComposeNavigation.navController?.navigateUp()
    }
)

/**
 * Navigate to the given destination. Configure behavior via [options].
 * @param navDestination The [NavDestination] to navigate
 */
fun <D : NavDestination> Modifier.navigateTo(
    navDestination: D,
    options: NavOptions.() -> Unit = {}
): Modifier = this.then(
    Modifier.clickable {
        HandleComposeNavigation.handleNavigateTo(navDestination, options)
    }
)

/**
 * Switch to a different tab destination.
 * Implemented to be singleTop + restoreState.
 */
fun <D : NavDestination> Modifier.switchTab(navDestination: D): Modifier = this.then(
    Modifier.clickable {
        HandleComposeNavigation.handleSwitchTo(navDestination)
    }
)

/**
 * Pop up to the [NavDestination] in the navigation stack.
 */
fun <D : NavDestination> Modifier.popBackTo(
    navDestination: D?,
    inclusive: Boolean
): Modifier = this.then(
    Modifier.clickable {
        HandleComposeNavigation.handlePopBackTo(navDestination, inclusive)
    }
)
