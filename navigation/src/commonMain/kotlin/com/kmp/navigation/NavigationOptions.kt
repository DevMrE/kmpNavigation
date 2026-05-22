package com.kmp.movieapp.core.util.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

data class NavigationOptions(
    val launchSingleTop: Boolean = false,
    val popUpTo: KClass<out NavKey>? = null,
    val inclusive: Boolean = false,
    val replace: Boolean = false
)