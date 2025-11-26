package com.kmp.navigation

import kotlinx.serialization.Serializable

/**
 * Marker interface for typed navigation destinations.
 *
 * How it's used:
 * - Each destination in your app should implement `NavDestination`.
 * - The destination type itself is used as the route.
 * - Mark the destination class with [Serializable]
 *
 * ```kotlin
 * // route without params
 * @Serializable
 * data object HomeScreenDestination : NavDestination
 *
 * // route with params
 * @Serializable
 * data class DetailScreenDestination(val someParam: Int) : NavDestination
 * ```
 */
interface NavDestination