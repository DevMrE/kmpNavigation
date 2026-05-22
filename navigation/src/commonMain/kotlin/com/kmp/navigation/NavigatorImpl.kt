package com.kmp.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import com.kmp.movieapp.core.util.navigation.NavigationOptions
import com.kmp.movieapp.core.util.navigation.Navigator
import kotlin.reflect.KClass

internal class NavigatorImpl<T : NavKey>(
    startDestination: T
) : Navigator<T> {

    override val backStack = mutableStateListOf(startDestination)

    override fun navigateTo(route: T, options: NavigationOptions) {
        val current = backStack.lastOrNull()

        // 1. Replace current screen
        if (options.replace && backStack.isNotEmpty()) {
            backStack.removeLast()
            backStack += route
            return
        }

        // 2. popUpTo behavior
        options.popUpTo?.let { clazz ->
            val index = backStack.indexOfLast {
                clazz.isInstance(it)
            }

            if (index != -1) {
                val target = if (options.inclusive) index else index + 1

                while (backStack.size > target) {
                    backStack.removeLast()
                }
            }
        }

        // 3. singleTop
        if (options.launchSingleTop && current == route) return

        // 4. default push
        backStack += route
    }

    override fun switchTo(route: T) {
        if (backStack.isNotEmpty()) {
            backStack.removeLast()
        }

        backStack += route
    }

    override fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    override fun popUpTo(
        clazz: KClass<out T>,
        inclusive: Boolean
    ) {

        val index = backStack.indexOfLast {
            clazz.isInstance(it)
        }

        if (index == -1) return

        val target =
            if (inclusive) index
            else index + 1

        while (backStack.size > target) {
            backStack.removeLast()
        }
    }
}