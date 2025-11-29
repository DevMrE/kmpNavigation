package com.kmp.navigation

import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Internal registry used by the typed graph builder and the navigation layer.
 *
 * It keeps:
 * - a mapping from route (Navigation-Compose typed route) → NavDestination KClass
 * - a mapping from NavDestination KClass → root KClass (for sections / tabs)
 *
 * This allows us to:
 * - resolve the current destination type from NavBackStackEntry.route
 * - know which "root section" a destination belongs to (for switchTab)
 */
object TypedDestinationRegistry {

    // route string -> destination KClass
    @PublishedApi
    internal val routeToClass = mutableMapOf<String, KClass<out NavDestination>>()

    // destination KClass -> root KClass (section parent or itself)
    @PublishedApi
    internal val classToRoot =
        mutableMapOf<KClass<out NavDestination>, KClass<out NavDestination>>()

    /**
     * Register a screen destination with its optional root section.
     *
     * @param rootClass the KClass of the root section parent, or null if this
     *                  screen is itself a root (top level destination).
     */
    @PublishedApi
    internal inline fun <reified NavDest : NavDestination> registerScreen(
        rootClass: KClass<out NavDestination>?
    ) {
        val destClass = NavDest::class
        val effectiveRoot = rootClass ?: destClass

        // track root mapping
        classToRoot[destClass] = effectiveRoot
        if (!classToRoot.containsKey(effectiveRoot)) {
            classToRoot[effectiveRoot] = effectiveRoot
        }

        // typed Navigation-Compose uses the kotlinx.serialization serialName as route
        val route = routeOf<NavDest>()
        routeToClass[route] = destClass
    }

    /**
     * Marks the given parent as a root section.
     *
     * All screens declared inside the corresponding `section<Parent, ...>` block
     * will use this parent as their root.
     */
    @PublishedApi
    internal inline fun <reified ParentNavDest : NavDestination> registerSectionRoot() {
        val parentClass = ParentNavDest::class
        classToRoot[parentClass] = parentClass
    }

    /**
     * Returns the destination KClass for the given Navigation route, or null
     * if this route has not been registered (e.g. non-typed destinations).
     */
    fun classForRoute(route: String): KClass<out NavDestination>? =
        routeToClass[route]

    /**
     * Returns the root KClass for a given destination type.
     *
     * If no explicit root mapping exists, the class is considered its own root.
     */
    fun rootForClass(destClass: KClass<out NavDestination>): KClass<out NavDestination> =
        classToRoot[destClass] ?: destClass

    @PublishedApi
    internal inline fun <reified NavDest : NavDestination> routeOf(): String =
        serializer<NavDest>().descriptor.serialName
}
