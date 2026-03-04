package com.kmp.navigation

object NavigationFactory {
    fun create(): Navigation = GlobalNavigation.navigation

    @PublishedApi
    internal fun controller(): NavigationController = GlobalNavigation.controller
}