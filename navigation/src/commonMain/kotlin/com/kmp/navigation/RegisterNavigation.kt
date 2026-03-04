package com.kmp.navigation

import co.touchlab.kermit.Logger
import kotlin.reflect.KClass

/**
 * Entry point for configuring the navigation graph.
 *
 * Call this once during app startup before rendering any Compose UI.
 *
 * ```kotlin
 * fun setupNavigation() {
 *     registerNavigation(startDestination = HomeContentDestination) {
 *
 *         content<HomeContentDestination> { HomeContent() }
 *         content<SettingsContentDestination> { SettingsContent() }
 *         content<MovieContentDestination> { MovieContent() }
 *         content<SeriesContentDestination> { SeriesContent() }
 *
 *         screen<DetailScreenDestination> { dest -> DetailScreen(dest.id) }
 *
 *         tabs<HomeTabs>(
 *             startDestination = MovieContentDestination,
 *             MovieContentDestination, SeriesContentDestination
 *         )
 *         tabs<AppRoot>(
 *             startDestination = HomeContentDestination,
 *             HomeContentDestination, SettingsContentDestination
 *         )
 *     }
 * }
 * ```
 */
fun registerNavigation(
    startDestination: NavDestination,
    builder: RegisterNavigationBuilder.() -> Unit
) {
    val dsl = RegisterNavigationBuilder().apply(builder)

    // Build group parent lookup:
    // For each group G, find if any destination in another group references G
    // by checking if the destination's screen contains NavigationContent<G>.
    // Since we can't inspect composables, we infer nesting from the group structure:
    // A destination D is a "parent" of group G if D is in a group AND
    // NavigationContent<G> would be called from D's composable.
    //
    // We cannot infer this automatically – developers must declare nesting explicitly
    // OR we infer it from startDestination chain:
    // If startDestination is in group G, and G has a parent group P,
    // then the parent destination in P that renders G must be specified.
    //
    // For now: we resolve parent-child group relationships by finding
    // destinations that appear in multiple groups (not possible by design),
    // OR by the startDestination chain.
    //
    // The simplest correct approach: infer group parents from startDestination.
    // startDestination = MovieContentDestination
    // MovieContentDestination → belongs to HomeTabs
    // HomeTabs startDestination = MovieContentDestination
    // AppRoot destinations include HomeContentDestination
    // HomeContentDestination is NOT in HomeTabs → HomeContentDestination is parent of HomeTabs
    //
    // Algorithm:
    // For each group G, check if any destination in OTHER groups contains
    // a destination from G in their tabs. This is found by checking
    // which destinations are NOT in any group but ARE referenced as
    // the "container" for sub-groups.
    //
    // Simplified: trust that startDestination resolves the chain.

    val groupParents = resolveGroupParents(dsl)

    NavigationGraph.configure(
        destinationMap = dsl.destinationMap,
        tabGroupMap = dsl.tabGroupMap,
        destToGroup = dsl.destToGroup
    )

    val controller = GlobalNavigation.controller
    controller.setGroupParents(groupParents)
    controller.initialize(startDestination)
}

/**
 * Resolves group parent destinations.
 *
 * For each tabs group G, finds the destination in a parent group
 * that "owns" group G (i.e., its composable contains NavigationContent<G>).
 *
 * Since we cannot inspect composables at runtime, we infer this from
 * the group structure:
 * - If group G's destinations are NOT in any other group → G is a root group
 * - If a destination D in group P is NOT in any group itself but renders G,
 *   we find D by looking at destinations in P that are not in G
 *
 * For the common case (AppRoot contains HomeContentDestination which renders HomeTabs):
 * - AppRoot destinations: [HomeContentDestination, SettingsContentDestination]
 * - HomeTabs destinations: [MovieContentDestination, SeriesContentDestination]
 * - HomeContentDestination is in AppRoot but NOT in HomeTabs
 * - So HomeContentDestination is a candidate parent for HomeTabs
 *
 * We determine the parent by finding which destination in a parent group
 * has the same startDestination chain as the sub-group's startDestination.
 *
 * Simplified algorithm:
 * For each group G with startDestination S:
 *   Find group P where P contains destination D, and D is NOT in G,
 *   and D's group startDestination chain leads to S.
 *
 * Since this is complex to automate fully, we use a pragmatic approach:
 * The parent of group G is the destination in another group that shares
 * the same "path" as G's start destination.
 */
private fun resolveGroupParents(
    dsl: RegisterNavigationBuilder
): Map<KClass<out NavGroup>, NavDestination> {
    val result = mutableMapOf<KClass<out NavGroup>, NavDestination>()

    // For each group G, find a destination D in another group P such that:
    // D is not in G's destinations, but D is in P's destinations
    // AND D is registered as content/screen (not a tab destination itself at top level)

    val allTabDestClasses = dsl.destToGroup.keys

    dsl.tabGroupMap.forEach { (groupClass, tabsData) ->
        val groupDestClasses = tabsData.destinations.map { it::class }.toSet()

        // Find a group P whose destinations include a destination D
        // where D is NOT in this group's destinations
        // AND D's composable would render NavigationContent<G>
        //
        // Since we can't inspect composables, we look for:
        // A destination D that is in some group P,
        // where D is a content destination (not itself a tab in another sub-group),
        // and the startDestination of G is reachable from D.
        //
        // Pragmatic heuristic: find groups that do NOT contain any of G's destinations
        // The destination in that group that shares the closest "namespace" to G's start
        // is the parent.

        dsl.tabGroupMap.forEach parentSearch@{ (parentGroupClass, parentTabsData) ->
            if (parentGroupClass == groupClass) return@parentSearch

            // Check if parent group contains any destination that could parent G
            parentTabsData.destinations.forEach { parentDest ->
                if (!groupDestClasses.contains(parentDest::class) &&
                    !allTabDestClasses.contains(parentDest::class)
                ) {
                    // parentDest is a content/screen destination in parentGroup
                    // that is not itself a tab → it's a candidate parent for G
                    // Only set if not already found (first match wins)
                    if (!result.containsKey(groupClass)) {
                        result[groupClass] = parentDest
                        Logger.i("registerNavigation") {
                            "Group ${groupClass.simpleName} parent → " +
                                    "${parentDest::class.simpleName} (in ${parentGroupClass.simpleName})"
                        }
                    }
                }
            }
        }
    }

    return result
}