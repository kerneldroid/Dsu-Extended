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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * Material 3 Expressive Animation System
 * Spring-based animations for natural, responsive feel
 */
object DSUAnimations {

    // Duration constants
    private const val DURATION_SHORT = 150
    private const val DURATION_MEDIUM = 300
    private const val DURATION_LONG = 500
    private const val DURATION_EXTRA_LONG = 700

    // ═══════════════════════════════════════════════════════════════
    // Spring Configurations
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bouncy spring for playful, expressive animations
     */
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /**
     * Gentle spring for subtle, smooth animations
     */
    val GentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow,
    )

    /**
     * Snappy spring for quick, responsive animations
     */
    val SnappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    /**
     * Default spring for general use
     */
    val DefaultSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    // ═══════════════════════════════════════════════════════════════
    // Card Animations
    // ═══════════════════════════════════════════════════════════════

    /**
     * Card enter animation - fade + scale up
     */
    fun cardEnterAnimation(index: Int = 0): EnterTransition {
        return fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialScale = 0.92f,
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialOffsetY = { it / 10 },
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Content Transitions
    // ═══════════════════════════════════════════════════════════════

    /**
     * Smooth content transition for state changes
     */
    val contentTransition: ContentTransform = fadeIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
    ) + scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        initialScale = 0.95f,
    ) togetherWith fadeOut(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
    ) + scaleOut(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        targetScale = 0.95f,
    )

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

/**
 * Constants for haptic feedback
 */
object HapticConstants {
    const val TICK_DURATION = 10L
    const val CLICK_DURATION = 20L
    const val HEAVY_CLICK_DURATION = 30L
    const val DOUBLE_CLICK_DURATION = 40L
}
