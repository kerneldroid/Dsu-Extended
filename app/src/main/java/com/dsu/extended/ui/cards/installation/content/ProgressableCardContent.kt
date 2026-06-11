package com.dsu.extended.ui.cards.installation.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dsu.extended.ui.components.buttons.PrimaryButton
import com.dsu.extended.ui.components.buttons.SecondaryButton
import com.dsu.extended.ui.components.ExpressiveIndeterminateLoadingBar
import com.dsu.extended.ui.components.ExpressiveProgressBar
import com.dsu.extended.ui.theme.DSUShapes
import com.dsu.extended.ui.theme.DSUTextStyles
import com.dsu.extended.ui.theme.LocalUiStyle
import com.dsu.extended.ui.theme.SemanticColors
import com.dsu.extended.ui.theme.UiStyle
import com.dsu.extended.ui.theme.AppFontFamily

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressableCardContent(
    text: String,
    showProgressBar: Boolean = false,
    isIndeterminate: Boolean = false,
    progress: Float = 0F,
    textFirstButton: String = "",
    textSecondButton: String = "",
    onClickFirstButton: (() -> Unit)? = null,
    onClickSecondButton: (() -> Unit)? = null,
    showSuccess: Boolean = false,
    showError: Boolean = false,
    suggestion: String = "",
    auxActionIcon: ImageVector = Icons.Rounded.Description,
    auxActionContentDescription: String = "",
    onClickAuxAction: (() -> Unit)? = null,
) {
    val uiStyle = LocalUiStyle.current
    val safeProgress = progress.coerceIn(0f, 1f)
    val sizeAnimationSpec = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val progressAnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = 280, easing = FastOutSlowInEasing)

    val progressTextStyle =
        DSUTextStyles.progressText.copy(
            fontFamily = AppFontFamily,
        )
    val progressColor = MaterialTheme.colorScheme.primary
    val progressTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)

    val successIconScale by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0.78f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "successIconScale",
    )
    val successPulseTransition = rememberInfiniteTransition(label = "successPulse")
    val successPulse by successPulseTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "successPulseScale",
    )
    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = sizeAnimationSpec,
        ),
    ) {
        // Success / Error icon
        AnimatedVisibility(
            visible = showSuccess || showError,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (showSuccess) {
                                SemanticColors.successContainer()
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (showSuccess) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.Error
                        },
                        contentDescription = null,
                        tint = if (showSuccess) {
                            SemanticColors.success()
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                if (showSuccess) {
                                    scaleX = successIconScale * successPulse
                                    scaleY = successIconScale * successPulse
                                }
                            },
                    )
                }
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                Text(
                    text = if (showSuccess) "Success!" else "Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (showSuccess) {
                        SemanticColors.success()
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }

        // Main text
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Progress bar
        AnimatedVisibility(
            visible = showProgressBar,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            ) {
                // Progress percentage
                if (!isIndeterminate) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = safeProgress,
                        animationSpec = progressAnimationSpec,
                        label = "progress",
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = progressTextStyle,
                            color = progressColor,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (isIndeterminate) {
                    ExpressiveIndeterminateLoadingBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        progressColor = progressColor,
                        trackColor = progressTrackColor,
                    )
                } else {
                    ExpressiveProgressBar(
                        progress = safeProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        progressColor = progressColor,
                        trackColor = progressTrackColor,
                    )
                }
            }
        }

        // Suggestion box
        AnimatedVisibility(
            visible = suggestion.isNotEmpty(),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(DSUShapes.ChipShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                    .padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 4.dp),
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = suggestion,
                        style = DSUTextStyles.suggestionText,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 16.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClickAuxAction != null) {
                IconButton(
                    onClick = onClickAuxAction,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    Icon(
                        imageVector = auxActionIcon,
                        contentDescription = auxActionContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.padding(end = 4.dp))
            }
            Spacer(modifier = Modifier.weight(1F))
            if (onClickSecondButton != null) {
                SecondaryButton(
                    text = textSecondButton,
                    onClick = onClickSecondButton,
                )
            }
            if (onClickFirstButton != null && onClickSecondButton != null) {
                Spacer(modifier = Modifier.padding(end = 8.dp))
            }
            if (onClickFirstButton != null) {
                PrimaryButton(
                    text = textFirstButton,
                    onClick = onClickFirstButton,
                )
            }
        }
    }
}
