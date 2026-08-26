package com.dsu.extended.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Material 3 Expressive Animation System
 * Spring-based animations for natural, responsive feel
 */
object DSUAnimations {

    // Duration constants
    private const val DURATION_MEDIUM = 300

    /**
     * Screen transitions - Modern Stack Style
     */
    val screenEnterAnimation: EnterTransition = slideInHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        initialOffsetX = { it }
    ) + fadeIn(
        animationSpec = tween(DURATION_MEDIUM)
    )

    val screenExitAnimation: ExitTransition = slideOutHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        targetOffsetX = { -it / 4 }
    ) + fadeOut(
        animationSpec = tween(DURATION_MEDIUM)
    ) + scaleOut(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        targetScale = 0.95f
    )

    val screenPopEnterAnimation: EnterTransition = slideInHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        initialOffsetX = { -it / 4 }
    ) + fadeIn(
        animationSpec = tween(DURATION_MEDIUM)
    ) + scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        initialScale = 0.95f
    )

    val screenPopExitAnimation: ExitTransition = slideOutHorizontally(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        targetOffsetX = { it }
    ) + fadeOut(
        animationSpec = tween(DURATION_MEDIUM)
    )
}
