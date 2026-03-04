package com.kmp.navigation

import kotlinx.atomicfu.AtomicRef

/**
 * Describes the last navigation operation.
 * Used by NavigationContent to decide which animation to play.
 */
enum class NavigationEvent {
    Idle,
    NavigateTo,
    NavigateUp,
    PopBackTo,
    SwitchTab,
    ClearStack
}