package com.kmp.navigation

/**
 * Marker interface for navigation groups (tabs).
 *
 * Groups define which destinations share a display area.
 * Switching between group destinations does NOT add to the BackStack.
 *
 * ```kotlin
 * interface HomeTabs : NavTab
 * interface AppRoot : NavTab
 * ```
 */
interface NavTabs