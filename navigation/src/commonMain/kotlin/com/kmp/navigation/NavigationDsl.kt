package com.kmp.navigation

/**
 * DSL marker used to avoid accidentally mixing builder scopes.
 *
 * This is declared exactly once in the project and reused in all
 * navigation-related DSL builders.
 *
 * ```kotlin
 * @NavigationDsl
 * class RegisterNavigationBuilder { /* ... */ }
 * ```
 */
@DslMarker
annotation class NavigationDsl
