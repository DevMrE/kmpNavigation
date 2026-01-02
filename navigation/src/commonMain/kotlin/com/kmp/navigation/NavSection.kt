package com.kmp.navigation

import androidx.compose.runtime.Stable

/**
 * Marker interface for a logical navigation section (e.g. "Home", "Auth", "Settings").
 *
 * ```kotlin
 * object HomeSection : NavSection
 * object AuthSection : NavSection
 * object SettingsSection : NavSection
 * ```
 */
@Stable
interface NavSection
