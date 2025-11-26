package com.kmp.navigation

import androidx.navigation.serialization.generateHashCode
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

object DefaultRouteIdProvider : RouteIdProvider {

    @OptIn(InternalSerializationApi::class)
    override fun idFor(clazz: KClass<out NavDestination>): Int =
        clazz.serializer().generateHashCode()
}