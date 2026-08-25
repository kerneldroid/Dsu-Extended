package com.dsu.extended.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FullWidthNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

private val IndicatorShape = RoundedCornerShape(16.dp)

@Composable
fun FullWidthNavBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    items: List<FullWidthNavItem>,
    modifier: Modifier = Modifier,
    height: Dp = 88.dp,
    cornerRadius: Dp = 28.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    selectedColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    val shape = remember(cornerRadius) {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(height)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                FullWidthNavItem(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelected(index) },
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                    indicatorColor = indicatorColor,
                )
            }
        }
    }
}

@Composable
private fun RowScope.FullWidthNavItem(
    item: FullWidthNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    indicatorColor: Color,
) {
    val colorAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Color>()
    val spatialAnimationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    val animatedColor by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = colorAnimationSpec,
        label = "NavItemColor",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "NavItemIconScale",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val lift by animateDpAsState(
        targetValue = if (pressed) (-3).dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>(),
        label = "NavItemLift",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer { translationY = lift.toPx() }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .semantics(mergeDescendants = true) {
                this.role = Role.Tab
                this.selected = selected
                this.contentDescription = item.label
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(width = 64.dp, height = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(100)) +
                    scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    ),
                exit = fadeOut(animationSpec = tween(100)) +
                    scaleOut(animationSpec = tween(100)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .background(color = indicatorColor, shape = IndicatorShape),
                )
            }

            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = animatedColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
        }

        Text(
            text = item.label,
            color = animatedColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
