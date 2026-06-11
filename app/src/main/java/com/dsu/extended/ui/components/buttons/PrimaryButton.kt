package com.dsu.extended.ui.components.buttons

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dsu.extended.ui.theme.DSUTextStyles

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    glow: Boolean = false,
) {
    val baseShape = RoundedCornerShape(18.dp)
    val pressedShape = RoundedCornerShape(10.dp)
    val isStrongGlow = glow && isEnabled
    val glowColor = MaterialTheme.colorScheme.primary.copy(
        alpha =
            if (isStrongGlow) {
                0.46f
            } else if (isEnabled) {
                0.14f
            } else {
                0f
            },
    )
    val glowElevation =
        when {
            !isEnabled -> 0.dp
            isStrongGlow -> 10.dp
            else -> 4.dp
        }
    val glowModifier =
        if (glowElevation > 0.dp) {
            Modifier.shadow(
                elevation = glowElevation,
                shape = baseShape,
                ambientColor = glowColor,
                spotColor = glowColor,
                clip = false,
            )
        } else {
            Modifier
        }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "primaryButtonScale",
    )
    Button(
        modifier = glowModifier
            .then(modifier)
            .defaultMinSize(minHeight = 46.dp)
            .scale(scale),
        onClick = onClick,
        enabled = isEnabled,
        interactionSource = interactionSource,
        shapes = ButtonDefaults.shapes(
            shape = baseShape,
            pressedShape = pressedShape,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
    ) {
        Text(
            text = text,
            style = DSUTextStyles.buttonText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
