package com.kmp.navigation

/**
 * Convenience alias for [NavigationGraph.configureNavigationGraph].
 *
 * Call it once during app setup.
 */
fun registerNavigation(
    startDestination: NavDestination,
    builder: RegisterNavigationBuilder.() -> Unit
) {
    NavigationGraph.configureNavigationGraph(startDestination, builder)
}

/**
 * Overload: register navigation + screen strategies in one call.
 */
fun registerNavigation(
    startDestination: NavDestination,
    screenStrategies: ScreenStrategiesBuilder.() -> Unit,
    builder: RegisterNavigationBuilder.() -> Unit
) {
    ScreenStrategiesBuilder().apply(screenStrategies)
    NavigationGraph.configureNavigationGraph(startDestination, builder)
}

@NavigationDsl
class ScreenStrategiesBuilder {

    fun registerScreenStrategyForType(type: ScreenStrategyType, strategy: ScreenStrategy) {
        ScreenStrategyRegistry.registerScreenStrategyForType(type, strategy)
    }

    fun mobile(strategy: ScreenStrategy) =
        registerScreenStrategyForType(ScreenStrategyType.MOBILE, strategy)

    fun tabletPortrait(strategy: ScreenStrategy) =
        registerScreenStrategyForType(ScreenStrategyType.TABLET_PORTRAIT, strategy)

    fun tabletLandscape(strategy: ScreenStrategy) =
        registerScreenStrategyForType(ScreenStrategyType.TABLET_LANDSCAPE, strategy)

    fun desktop(strategy: ScreenStrategy) =
        registerScreenStrategyForType(ScreenStrategyType.DESKTOP, strategy)
}
