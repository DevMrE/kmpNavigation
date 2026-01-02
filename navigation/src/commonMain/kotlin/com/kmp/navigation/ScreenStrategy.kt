package com.kmp.navigation

enum class NavigationBarPosition {
    Bottom,
    Left,
    None
}

data class TwoPaneConfig(
    val enabled: Boolean = false,
    /**
     * Anteil für die linke (Primary/Master) Pane, z.B. 0.55f = 55%
     */
    val primaryPaneFraction: Float = 0.55f,
    /**
     * Optionales "genug Platz" Gate. Wenn du Type bereits sauber resolvest,
     * kann das bei 0 bleiben. (Dp)
     */
    val minWidthDp: Float = 0f
)

data class ScreenStrategy(
    val navBarPosition: NavigationBarPosition = NavigationBarPosition.Bottom,
    /**
     * Prozent-Anteil der NavBar: bei Bottom = Höhe, bei Left = Breite.
     * Beispiel: 0.12f => 12%
     */
    val navBarFraction: Float = 0.12f,
    val twoPane: TwoPaneConfig = TwoPaneConfig()
) {
    init {
        require(navBarFraction in 0f..1f) { "navBarFraction must be in [0..1], was $navBarFraction" }
    }
}
