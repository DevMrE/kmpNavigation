package com.devmre.kmpnavigation

import kotlin.reflect.KClass

fun interface RouteIdProvider {
    fun idFor(clazz: KClass<out NavDestination>): Int
}