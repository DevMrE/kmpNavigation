package com.kmp.navigation

/**
 * Marker interface for grouping destinations into type-safe "sections" / graphs.
 *
 * A section is purely a **compile-time concept** that allows you to:
 *
 * * Group destinations that logically belong together.
 * * Keep your navigation DSL readable and structured.
 *
 * At runtime the library uses only concrete [NavDestination] types as routes.
 *
 * ```kotlin
 * // Define sections for your app
 * object HomeSection : TypedGraph
 * object AuthSection : TypedGraph
 *
 * // Use them inside RegisterNavigation
 * @Composable
 * fun AppScreen() {
 *     RegisterNavigation(
 *         startDestination = HomeScreenDestination,
 *         builder = {
 *             section<HomeSection, HomeScreenDestination> {
 *                 screen<HomeScreenDestination> { HomeScreen() }
 *                 screen<SettingsScreenDestination> { SettingsScreen() }
 *             }
 *
 *             section<AuthSection, LoginDestination> {
 *                 screen<LoginDestination> { LoginScreen() }
 *                 screen<RegisterDestination> { RegisterScreen() }
 *             }
 *         }
 *     ) {
 *         NavigationHost()
 *     }
 * }
 * ```
 */
interface Section
