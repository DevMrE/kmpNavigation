package com.kmp.navigation

import androidx.navigation3.runtime.NavKey
import com.kmp.movieapp.core.util.navigation.Navigator

class DefaultNavigatorFactory<T : NavKey> : NavigatorFactory<T> {

    override fun create(start: T): Navigator<T> {
        return NavigatorImpl(start)
    }
}