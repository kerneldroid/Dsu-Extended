package com.dsu.extended.ui.cards.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsu.extended.BuildConfig
import com.dsu.extended.R
import com.dsu.extended.ui.components.SimpleCard
import com.dsu.extended.ui.components.buttons.PrimaryButton
import com.dsu.extended.ui.components.buttons.SecondaryButton
import com.dsu.extended.ui.screen.about.UpdateStatus
import com.dsu.extended.ui.screen.about.UpdaterCardState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdaterCard(
    uiState: UpdaterCardState,
    isUpdaterAvailable: Boolean,
    onClickImage: () -> Unit,
    onClickCheckUpdates: () -> Unit,
    onClickDownloadUpdate: () -> Unit,
    onClickViewChangelog: () -> Unit,
) {
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    fun isDownloading(): Boolean =
        uiState.isDownloading || uiState.updateStatus == UpdateStatus.CHECKING_FOR_UPDATES

    fun isCheckingForUpdates(): Boolean =
        uiState.updateStatus == UpdateStatus.CHECKING_FOR_UPDATES

    fun isUpdateFound(): Boolean =
        uiState.updateStatus == UpdateStatus.UPDATE_FOUND

    val topContourTransition = rememberInfiniteTransition(label = "topIconContourTransition")
    val topContourRotation by topContourTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "topIconContourRotation",
    )
    val topContourPulse by topContourTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "topIconContourPulse",
    )
    val topContourColor by animateColorAsState(
        targetValue = when {
            isCheckingForUpdates() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
            isLightTheme -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "topIconContourColor",
    )
    val topContourShape =
        if (isCheckingForUpdates()) {
            MaterialShapes.Cookie9Sided.toShape(startAngle = topContourRotation.roundToInt())
        } else {
            MaterialShapes.Cookie7Sided.toShape(startAngle = 0)
        }

    SimpleCard(
        addPadding = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .graphicsLayer {
                            if (isCheckingForUpdates()) {
                                val pulse = 1f + (0.05f * topContourPulse)
                                scaleX = pulse
                                scaleY = pulse
                            }
                        }
                        .clip(topContourShape)
                        .border(
                            border = BorderStroke(width = 1.8.dp, color = topContourColor),
                            shape = topContourShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(82.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Box {
                            val progressBarModifier = Modifier
                                .size(82.dp)
                                .align(Alignment.Center)
                            if (uiState.isDownloading) {
                                CircularProgressIndicator(
                                    progress = { uiState.progressBar },
                                    modifier = progressBarModifier,
                                )
                            }

                            val scale by animateFloatAsState(
                                targetValue = if (isDownloading()) 0.90f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                                label = "updaterIconScale",
                            )
                            Icon(
                                modifier = Modifier
                                    .size(74.dp)
                                    .scale(scale)
                                    .padding(14.dp)
                                    .align(Alignment.Center)
                                    .clickable { onClickImage() },
                                painter = painterResource(id = R.drawable.app_icon_mono),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = "App icon",
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    id = R.string.version_info,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.75f),
            )
        }
        Spacer(modifier = Modifier.padding(3.dp))
        if (isUpdaterAvailable) {
            CheckUpdatesExpressiveItem(
                uiState = uiState,
                onClick = onClickCheckUpdates,
            )
            AnimatedVisibility(visible = isUpdateFound()) {
                Row(
                    modifier = Modifier
                        .padding(all = 12.dp)
                        .padding(end = 4.dp),
                ) {
                    Spacer(modifier = Modifier.weight(1F))
                    SecondaryButton(
                        text = stringResource(id = R.string.changelog),
                        onClick = { onClickViewChangelog() },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    PrimaryButton(
                        text = stringResource(id = R.string.download),
                        onClick = { onClickDownloadUpdate() },
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.padding(6.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CheckUpdatesExpressiveItem(
    uiState: UpdaterCardState,
    onClick: () -> Unit,
) {
    val isChecking = uiState.updateStatus == UpdateStatus.CHECKING_FOR_UPDATES
    val description =
        when (uiState.updateStatus) {
            UpdateStatus.NO_UPDATE_FOUND -> stringResource(id = R.string.check_updates_text_updated)
            UpdateStatus.UPDATE_FOUND -> stringResource(R.string.check_updates_text_found, uiState.updateVersion)
            else -> stringResource(id = R.string.check_updates_text_idle)
        }
    val backgroundColor by animateColorAsState(
        targetValue =
            if (isChecking) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                Color.Transparent
            },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "checkUpdatesBackground",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(enabled = !isChecking) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.check_updates_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}
