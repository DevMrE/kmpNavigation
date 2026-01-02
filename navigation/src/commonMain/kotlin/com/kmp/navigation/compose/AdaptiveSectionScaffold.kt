package com.kmp.navigation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.kmp.navigation.NavSection
import com.kmp.navigation.NavigationBarPosition
import com.kmp.navigation.ScreenStrategy
import com.kmp.navigation.ScreenStrategyRegistry
import com.kmp.navigation.ScreenStrategyType

data class ScreenSizeDp(val widthDp: Float, val heightDp: Float)

val LocalScreenStrategyType =
    staticCompositionLocalOf { ScreenStrategyType.MOBILE }

val LocalScreenStrategy =
    staticCompositionLocalOf { ScreenStrategy() }

val LocalScreenSizeDp =
    staticCompositionLocalOf { ScreenSizeDp(0f, 0f) }

/**
 * Provides [LocalScreenStrategyType], [LocalScreenStrategy], [LocalScreenSizeDp]
 * based on current constraints (KMP-friendly: Android/iOS/Desktop).
 */
@Composable
fun ProvideScreenStrategy(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value

        val type = ScreenStrategyRegistry.resolveType(widthDp, heightDp)
        val strategy = ScreenStrategyRegistry.strategyFor(type)

        CompositionLocalProvider(
            LocalScreenStrategyType provides type,
            LocalScreenStrategy provides strategy,
            LocalScreenSizeDp provides ScreenSizeDp(widthDp, heightDp)
        ) {
            content()
        }
    }
}

/**
 * Root/Section host helper that places the "navigation bar" automatically
 * based on the current [ScreenStrategy], and shows the active child section.
 *
 * Typical use:
 * - root host destination screen (BottomBar host)
 * - or a tab host screen
 */
@Composable
fun AdaptiveSectionScaffold(
    parentSection: NavSection,
    modifier: Modifier = Modifier,
    navigationBar: @Composable (type: ScreenStrategyType, strategy: ScreenStrategy) -> Unit,
    contentModifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
) {
    val type = LocalScreenStrategyType.current
    val strategy = LocalScreenStrategy.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when (strategy.navBarPosition) {
            NavigationBarPosition.Bottom -> {
                val barHeight = maxHeight * strategy.navBarFraction
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        NavChildSectionsHost(
                            parentSection = parentSection,
                            modifier = contentModifier.fillMaxSize(),
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(barHeight)) {
                        navigationBar(type, strategy)
                    }
                }
            }

            NavigationBarPosition.Left -> {
                val barWidth = maxWidth * strategy.navBarFraction
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(barWidth).fillMaxHeight()) {
                        navigationBar(type, strategy)
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        NavChildSectionsHost(
                            parentSection = parentSection,
                            modifier = contentModifier.fillMaxSize(),
                        )
                    }
                }
            }

            NavigationBarPosition.None -> {
                NavChildSectionsHost(
                    parentSection = parentSection,
                    modifier = contentModifier.fillMaxSize(),
                )
            }
        }
    }
}
