package com.kmp.kmpnavigation.util

import kotlin.reflect.KClass

fun interface RouteIdProvider {
    fun idFor(clazz: KClass<out NavDestination>): Int
}