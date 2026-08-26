package com.dsu.extended.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dsu.extended.ui.theme.DSUShapes
import com.dsu.extended.ui.theme.GradientColors

/**
 * Expressive card types
 */
enum class CardVariant {
    DEFAULT,
    ELEVATED,
    FILLED,
    OUTLINED,
    GRADIENT_PRIMARY,
    GRADIENT_SECONDARY,
    GRADIENT_SUCCESS,
    GRADIENT_ERROR,
    GRADIENT_WARNING,
}

/**
 * Material 3 Expressive Card Box Component
 * Features: larger corner radius, shadows, gradients, animations
 */
@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    cardTitle: String = "",
    addToggle: Boolean = false,
    isToggleChecked: Boolean = false,
    isToggleEnabled: Boolean = true,
    addPadding: Boolean = true,
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    roundedCornerShape: RoundedCornerShape = DSUShapes.CardShape,
    variant: CardVariant = CardVariant.DEFAULT,
    elevation: Dp = 0.dp,
    onCheckedChange: ((Boolean) -> Unit) = {},
    content: @Composable (ColumnScope) -> Unit,
) {
    val backgroundModifier = when (variant) {
        CardVariant.DEFAULT -> Modifier.background(cardColor)

        CardVariant.ELEVATED ->
            Modifier
                .shadow(
                    elevation = 4.dp,
                    shape = roundedCornerShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                )
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))

        CardVariant.FILLED -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)

        CardVariant.OUTLINED ->
            Modifier
                .background(Color.Transparent)
                .then(
                    Modifier.background(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = roundedCornerShape,
                    ),
                )

        CardVariant.GRADIENT_PRIMARY -> Modifier.background(
            brush = Brush.linearGradient(GradientColors.PrimaryGradient),
        )

        CardVariant.GRADIENT_SECONDARY -> Modifier.background(
            brush = Brush.linearGradient(GradientColors.SecondaryGradient),
        )

        CardVariant.GRADIENT_SUCCESS -> Modifier.background(
            brush = Brush.linearGradient(GradientColors.SuccessGradient),
        )

        CardVariant.GRADIENT_ERROR -> Modifier.background(
            brush = Brush.linearGradient(GradientColors.ErrorGradient),
        )

        CardVariant.GRADIENT_WARNING -> Modifier.background(
            brush = Brush.linearGradient(GradientColors.WarningGradient),
        )
    }

    val isGradient = variant.name.startsWith("GRADIENT")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            .clip(roundedCornerShape)
            .then(backgroundModifier)
            .then(
                if (addPadding) {
                    Modifier
                        .padding(
                            horizontal = 16.dp,
                            vertical = 16.dp,
                        )
                } else {
                    Modifier
                },
            )
    ) {
        Column {
            if (cardTitle.isNotEmpty()) {
                if (addToggle) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardTitle(
                            modifier = Modifier.weight(1F),
                            cardTitle = cardTitle,
                            color = if (isGradient) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Switch(
                            checked = isToggleChecked,
                            onCheckedChange = onCheckedChange,
                            enabled = isToggleEnabled,
                            thumbContent = {
                                if (isToggleChecked) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (isGradient) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = if (isGradient) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            },
                            colors = if (isGradient) {
                                SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color.White.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                                )
                            } else {
                                SwitchDefaults.colors()
                            },
                        )
                    }
                } else {
                    CardTitle(
                        cardTitle = cardTitle,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                        color = if (isGradient) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            content(this)
        }
    }
}

/**
 * Elevated card variant for primary content
 */

/**
 * Gradient card for accented content
 */

/**
 * Installation progress card with larger shape
 */
