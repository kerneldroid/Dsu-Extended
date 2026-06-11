package com.dsu.extended.ui.components.buttons

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dsu.extended.ui.theme.DSUTextStyles

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    colorButton: Color? = null,
    colorText: Color? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    textButton: Boolean = false,
    isEnabled: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val resolvedTextColor = colorText ?: MaterialTheme.colorScheme.onSecondaryContainer
    val resolvedIconTint = iconTint ?: colorText ?: MaterialTheme.colorScheme.primary
    val baseShape = RoundedCornerShape(18.dp)
    val pressedShape = RoundedCornerShape(10.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "actionButtonScale",
    )

    if (textButton) {
        TextButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            interactionSource = interactionSource,
            shapes = ButtonDefaults.shapes(
                shape = baseShape,
                pressedShape = pressedShape,
            ),
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = colorText ?: MaterialTheme.colorScheme.primary,
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedIconTint,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    style = DSUTextStyles.buttonText,
                    color = colorText ?: MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        FilledTonalButton(
            modifier = modifier
                .defaultMinSize(minHeight = 46.dp)
                .scale(scale),
            onClick = onClick,
            enabled = isEnabled,
            interactionSource = interactionSource,
            shapes = ButtonDefaults.shapes(
                shape = baseShape,
                pressedShape = pressedShape,
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
            colors = if (colorButton != null) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = colorButton,
                    contentColor = resolvedTextColor,
                )
            } else {
                ButtonDefaults.filledTonalButtonColors()
            },
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedIconTint,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = DSUTextStyles.buttonText,
                color = resolvedTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            content()
        }
    }
}
