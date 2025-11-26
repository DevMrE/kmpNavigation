package com.devmre.kmpnavigation

/**
 * Marker interface for typed navigation destinations.
 *
 * How it's used:
 * - Each destination in your app should implement `NavDestination`.
 * - The destination type itself is used as the route when working with Jetpack Navigation Compose.
 * - The `Navigation` API is generic on `D : NavDestination` to keep calls type-safe.
 */
interface NavDestination