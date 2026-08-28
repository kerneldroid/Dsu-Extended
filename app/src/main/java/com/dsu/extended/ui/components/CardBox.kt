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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Built on the native androidx.compose.material3.Card.
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
    val isGradient = variant.name.startsWith("GRADIENT")
    val containerColor = when (variant) {
        CardVariant.DEFAULT -> cardColor
        CardVariant.ELEVATED -> MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        CardVariant.FILLED -> MaterialTheme.colorScheme.surfaceContainerHighest
        CardVariant.OUTLINED -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> Color.Transparent
    }
    val gradientBrush = when (variant) {
        CardVariant.GRADIENT_PRIMARY -> Brush.linearGradient(GradientColors.PrimaryGradient)
        CardVariant.GRADIENT_SECONDARY -> Brush.linearGradient(GradientColors.SecondaryGradient)
        CardVariant.GRADIENT_SUCCESS -> Brush.linearGradient(GradientColors.SuccessGradient)
        CardVariant.GRADIENT_ERROR -> Brush.linearGradient(GradientColors.ErrorGradient)
        CardVariant.GRADIENT_WARNING -> Brush.linearGradient(GradientColors.WarningGradient)
        else -> null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
        shape = roundedCornerShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (variant == CardVariant.ELEVATED && elevation == 0.dp) 4.dp else elevation,
        ),
    ) {
        Box {
            if (gradientBrush != null) {
                Box(modifier = Modifier.matchParentSize().background(brush = gradientBrush))
            }
            Column(
                modifier = Modifier.then(
                    if (addPadding) {
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    } else {
                        Modifier
                    },
                ),
            ) {
                if (cardTitle.isNotEmpty()) {
                    if (addToggle) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CardTitle(
                                modifier = Modifier.weight(1F),
                                cardTitle = cardTitle,
                                color = if (isGradient) Color.White else MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Switch(
                                checked = isToggleChecked,
                                onCheckedChange = onCheckedChange,
                                enabled = isToggleEnabled,
                                thumbContent = {
                                    if (isToggleChecked) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = if (isGradient) {
                                                Color.Black.copy(alpha = 0.6f)
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = if (isGradient) {
                                                Color.Black.copy(alpha = 0.6f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHighest
                                            },
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
                            modifier = Modifier.padding(top = 0.dp, bottom = 6.dp),
                            color = if (isGradient) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                content(this)
            }
        }
    }
}
