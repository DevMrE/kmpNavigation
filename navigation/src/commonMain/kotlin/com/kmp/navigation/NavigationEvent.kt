package com.kmp.navigation

/**
 * Describes the last navigation operation.
 * Used by NavigationContent to decide which animation to play.
 */
enum class NavigationEvent {
    Idle,
    NavigateTo,
    SwitchTo,
    NavigateUp,
    PopBackTo,
    ClearStack
}