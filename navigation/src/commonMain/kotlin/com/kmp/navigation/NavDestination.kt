package com.kmp.navigation

/**
 * Marker interface for typed navigation destinations.
 *
 * Every destination must be @Serializable and implement this interface.
 *
 * ```kotlin
 * @Serializable
 * data object HomeDestination : NavDestination
 *
 * @Serializable
 * data class DetailDestination(val id: String) : NavDestination
 * ```
 */
interface NavDestination