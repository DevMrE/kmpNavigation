package com.kmp.navigation

/**
 * Marker interface for a logical navigation section.
 *
 * Always implement as a singleton object:
 *
 * ```kotlin
 * @Serializable
 * data object HomeSection : NavSection
 *
 * @Serializable
 * data object SettingsSection : NavSection
 * ```
 */
interface NavSection