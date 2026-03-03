package com.kmp.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Defines enter and exit animations for a navigation transition.
 *
 * ```kotlin
 * NavTransitionSpec(
 *     enter = slideInHorizontally { it },
 *     exit = slideOutHorizontally { -it }
 * )
 * ```
 */
data class NavTransitionSpec(
    val enter: EnterTransition,
    val exit: ExitTransition
) {
    fun toContentTransform(): ContentTransform = enter togetherWith exit
}

/**
 * Default transitions used when no custom transition is provided.
 */
object NavTransitions {

    /** Simple fade – used for navigateTo and navigateUp */
    val fade = NavTransitionSpec(
        enter = fadeIn(),
        exit = fadeOut()
    )

    /** Slide from right – used for forward section switch */
    val slideInFromRight = NavTransitionSpec(
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { -it } + fadeOut()
    )

    /** Slide from left – used for backward section switch */
    val slideInFromLeft = NavTransitionSpec(
        enter = slideInHorizontally { -it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut()
    )
}