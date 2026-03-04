package com.kmp.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith


/**
 * Default transitions used when no custom transition is provided.
 */
@PublishedApi
internal object DefaultNavAnimations {

    /** Simple fade – used for navigateTo and navigateUp */
    val enterTransition: ContentTransform
        get() = slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()

    /** Slide from right – used for forward section switch */
    val popEnterTransition: ContentTransform
        get() = slideInHorizontally { -it } + fadeIn() togetherWith
                slideOutHorizontally { it } + fadeOut()

    /** Slide from left – used for backward section switch */
    val tabSwitchTransition: ContentTransform
        get() = fadeIn() togetherWith fadeOut()
}