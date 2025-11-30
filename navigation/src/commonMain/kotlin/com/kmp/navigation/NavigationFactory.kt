package com.kmp.navigation

import com.kmp.navigation.compose.NavigationImpl

/**
 * Factory, um die Standard-Implementierung zu kapseln.
 */
object NavigationFactory {
    fun create(): Navigation = NavigationImpl
}
