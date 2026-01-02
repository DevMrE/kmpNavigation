package com.kmp.navigation

/**
 * Global registry for screen strategies.
 *
 * - Consumers can register per [ScreenStrategyType]
 * - The compose layer resolves the current type from width/height dp
 * - If nothing is registered, defaults are used
 */
object ScreenStrategyRegistry {

    private val strategies = mutableMapOf<ScreenStrategyType, ScreenStrategy>()

    /**
     * Resolver: widthDp/heightDp -> ScreenStrategyType
     */
    private var typeResolver: (widthDp: Float, heightDp: Float) -> ScreenStrategyType =
        ::defaultResolver

    fun registerScreenStrategyForType(type: ScreenStrategyType, strategy: ScreenStrategy) {
        strategies[type] = strategy
    }

    fun registerMobileScreenStrategy(strategy: ScreenStrategy) =
        registerScreenStrategyForType(ScreenStrategyType.MOBILE, strategy)

    fun registerTabletPortraitScreenStrategy(strategy: ScreenStrategy) =
        registerScreenStrategyForType(ScreenStrategyType.TABLET_PORTRAIT, strategy)

    fun registerTabletLandscapeScreenStrategy(strategy: ScreenStrategy) =
        registerScreenStrategyForType(ScreenStrategyType.TABLET_LANDSCAPE, strategy)

    fun registerDesktopScreenStrategy(strategy: ScreenStrategy) =
        registerScreenStrategyForType(ScreenStrategyType.DESKTOP, strategy)

    fun setTypeResolver(resolver: (widthDp: Float, heightDp: Float) -> ScreenStrategyType) {
        typeResolver = resolver
    }

    fun resolveType(widthDp: Float, heightDp: Float): ScreenStrategyType =
        typeResolver(widthDp, heightDp)

    fun strategyFor(type: ScreenStrategyType): ScreenStrategy =
        strategies[type] ?: defaultStrategyFor(type)

    private fun defaultResolver(widthDp: Float, heightDp: Float): ScreenStrategyType {
        val isLandscape = widthDp >= heightDp
        return when {
            widthDp >= 1024f -> ScreenStrategyType.DESKTOP
            widthDp >= 600f && isLandscape -> ScreenStrategyType.TABLET_LANDSCAPE
            widthDp >= 600f -> ScreenStrategyType.TABLET_PORTRAIT
            else -> ScreenStrategyType.MOBILE
        }
    }

    private fun defaultStrategyFor(type: ScreenStrategyType): ScreenStrategy =
        when (type) {
            ScreenStrategyType.MOBILE ->
                ScreenStrategy(navBarPosition = NavigationBarPosition.Bottom, navBarFraction = 0.12f)

            ScreenStrategyType.TABLET_PORTRAIT ->
                ScreenStrategy(navBarPosition = NavigationBarPosition.Bottom, navBarFraction = 0.10f)

            ScreenStrategyType.TABLET_LANDSCAPE ->
                ScreenStrategy(
                    navBarPosition = NavigationBarPosition.Left,
                    navBarFraction = 0.18f,
                    twoPane = TwoPaneConfig(enabled = true, primaryPaneFraction = 0.55f, minWidthDp = 840f)
                )

            ScreenStrategyType.DESKTOP ->
                ScreenStrategy(
                    navBarPosition = NavigationBarPosition.Left,
                    navBarFraction = 0.20f,
                    twoPane = TwoPaneConfig(enabled = true, primaryPaneFraction = 0.55f, minWidthDp = 900f)
                )
        }
}
