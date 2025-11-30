package com.kmp.navigation

/**
 * Describes the last navigation operation that changed the back stack.
 *
 * The compose layer uses this to decide which animation to play,
 * e.g. fade for [NavigationEvent.NavigateTo] or horizontal slide
 * for [NavigationEvent.SwitchTo].
 */
enum class NavigationEvent {
    Idle,
    NavigateTo,
    SwitchTo,
    NavigateUp,
    PopBackTo
}