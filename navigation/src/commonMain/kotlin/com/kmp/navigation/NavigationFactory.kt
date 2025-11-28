package com.kmp.navigation

import com.kmp.navigation.compose.MutableComposeNavigation
import com.kmp.navigation.compose.NavigationImpl

/**
 * Entry point for obtaining the shared [Navigation] instance.
 *
 * Internally:
 * - A single [NavigationImpl] instance is created.
 * - It is used:
 *     - inside Compose (via [rememberNavDestination] / [RegisterNavigation])
 *     - in ViewModels via DI as [Navigation].
 *
 * Consumers should depend only on [Navigation]; implementation details
 * (NavigationImpl, MutableComposeNavigation) stay internal to the library.
 */
object NavigationFactory {

    private val impl = NavigationImpl()

    internal val mutableInstance: MutableComposeNavigation = impl

    fun create(): Navigation = impl
}
