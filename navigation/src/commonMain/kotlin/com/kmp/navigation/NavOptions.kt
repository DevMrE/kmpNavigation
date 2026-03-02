package com.kmp.navigation

/**
 * Options to influence how a navigation action is performed.
 *
 * - [singleTop]: avoid pushing the same destination on top if it is already current.
 * - [restoreState]: attempt to restore saved state when re-selecting a destination.
 * - [overlay]: if true, the target section renders on top of the current one.
 * - [backstack]: how the back stack should be adjusted for this navigation.
 */
data class NavOptions(
    var singleTop: Boolean = true,
    var restoreState: Boolean = false,
    var overlay: Boolean = true,
    var backstack: Backstack = Backstack.None,
) {
    /**
     * Back stack behavior for a navigation action.
     */
    sealed interface Backstack {
        /** Keep existing back stack. */
        data object None : Backstack

        /**
         * Pop up to [navDestination]. If [inclusive] is true,
         * also remove [navDestination].
         */
        data class PopTo(
            val navDestination: NavDestination,
            val inclusive: Boolean = false,
            val saveState: Boolean = false
        ) : Backstack

        /** Clear the whole stack. */
        data object Clear : Backstack
    }

    /** Configure to pop up to [navDestination] before navigating. */
    fun popTo(
        navDestination: NavDestination,
        inclusive: Boolean = false,
        saveState: Boolean = false
    ) {
        backstack = Backstack.PopTo(navDestination, inclusive, saveState)
    }

    /** Configure to clear the back stack before navigating. */
    fun clearStack() {
        backstack = Backstack.Clear
    }
}