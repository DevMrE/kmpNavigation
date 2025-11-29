package com.kmp.navigation

import kotlin.reflect.KClass

/**
 * Internal registry that maps each typed [NavDestination] class to its logical
 * "root" destination (usually the section parent).
 *
 * The mapping is built once while your typed graph is declared (via [screen]
 * and [section]) and later used by the navigation runtime to reason about
 * tab roots such as `Home` or `Settings`.
 */
object TypedDestinationRegistry {

    // Leaf (screen) -> root (section / itself)
    private val destToRoot: MutableMap<KClass<out NavDestination>, KClass<out NavDestination>> =
        mutableMapOf()

    /**
     * Register a leaf screen [destClass] and the optional [rootClass] it belongs to.
     *
     * If [rootClass] is null, the destination is considered its own root.
     */
    fun registerScreen(
        destClass: KClass<out NavDestination>,
        rootClass: KClass<out NavDestination>?
    ) {
        val effectiveRoot = rootClass ?: destClass

        destToRoot[destClass] = effectiveRoot
        // ensure root also maps to itself
        destToRoot.getOrPut(effectiveRoot) { effectiveRoot }
    }

    /**
     * Explicitly register a parent section root so it can later be used in
     * comparisons even if it has no dedicated [screen] entry itself.
     */
    fun registerSectionRoot(rootClass: KClass<out NavDestination>) {
        destToRoot.getOrPut(rootClass) { rootClass }
    }

    /**
     * Returns the logical root class for the given leaf [destClass].
     * If no mapping is known, null is returned.
     */
    internal fun rootClassOf(
        destClass: KClass<out NavDestination>
    ): KClass<out NavDestination>? = destToRoot[destClass]

    /**
     * Convenience: resolve the root for an actual [NavDestination] instance.
     */
    internal fun rootOf(dest: NavDestination): KClass<out NavDestination>? =
        rootClassOf(dest::class)
}
