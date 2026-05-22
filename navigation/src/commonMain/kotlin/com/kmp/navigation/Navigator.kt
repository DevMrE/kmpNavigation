package com.kmp.movieapp.core.util.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

interface Navigator<T : NavKey> {

    val backStack: SnapshotStateList<T>

    fun navigateTo(route: T, options: NavigationOptions = NavigationOptions())

    fun switchTo(route: T)

    fun navigateBack()

    fun popUpTo(clazz: KClass<out T>, inclusive: Boolean = false)
}