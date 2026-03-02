package com.kmp.navigation

import kotlinx.serialization.Serializable

/**
 * Marker interface for typed navigation destinations.
 *
 * ```kotlin
 * @Serializable
 * data object HomeScreenDestination : NavDestination
 *
 * @Serializable
 * data class DetailScreenDestination(val id: String) : NavDestination
 * ```
 */
interface NavDestination