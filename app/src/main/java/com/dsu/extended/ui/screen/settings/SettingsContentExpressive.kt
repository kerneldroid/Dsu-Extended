package com.dsu.extended.ui.screen.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import android.os.Build
import android.app.WallpaperManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dsu.extended.R
import com.dsu.extended.ui.components.MagiskIcon
import com.dsu.extended.ui.components.ShizukuIcon
import com.dsu.extended.ui.components.DhizukuIcon
import com.dsu.extended.ui.components.PreferenceItem
import com.dsu.extended.ui.theme.AppFontPreset
import com.dsu.extended.ui.theme.Primary
import com.dsu.extended.ui.theme.materialColorScheme
import com.dsu.extended.ui.theme.ColorPaletteStyle
import com.dsu.extended.ui.theme.ThemeMode
import com.dsu.extended.util.PreferredPrivilegedMode

data class SelectionOption(
    val key: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String = ""
)

@Composable
fun SettingsContentExpressive(
    uiState: SettingsUiState,
    settingsViewModel: SettingsViewModel,
    installationItems: List<@Composable (ListItemShapes) -> Unit>,
    developerItems: List<@Composable (ListItemShapes) -> Unit>,
    checkAllStatusRow: @Composable (ListItemShapes) -> Unit,
    aboutItem: @Composable (ListItemShapes) -> Unit,
    inspectorItem: @Composable (ListItemShapes) -> Unit,
    onOpenDialog: (DialogSheetState) -> Unit,
) {
    val expressiveOtherItems = buildList<@Composable (ListItemShapes) -> Unit> {
        add { shapes ->
            PreferenceItem(
                shapes = shapes,
                title = stringResource(id = R.string.operation_mode),
                description = settingsViewModel.checkOperationMode() + " · " + when (uiState.preferredPrivilegedMode) {
                    PreferredPrivilegedMode.ALL -> stringResource(id = R.string.operation_mode_preferred_all)
                    PreferredPrivilegedMode.AUTO -> stringResource(id = R.string.operation_mode_preferred_all)
                    PreferredPrivilegedMode.ROOT -> stringResource(id = R.string.operation_mode_preferred_root)
                    PreferredPrivilegedMode.SHIZUKU -> stringResource(id = R.string.operation_mode_preferred_shizuku)
                    PreferredPrivilegedMode.DHIZUKU -> stringResource(id = R.string.operation_mode_preferred_dhizuku)
                },
                icon = Icons.Rounded.Tune,
                onClick = {
                    onOpenDialog(DialogSheetState.OPERATION_MODE_SELECTOR)
                },
            )
        }
        add { shapes ->
            PreferenceItem(
                shapes = shapes,
                title = stringResource(id = R.string.theme_mode),
                description =
                when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> stringResource(id = R.string.theme_mode_system)
                    ThemeMode.LIGHT -> stringResource(id = R.string.theme_mode_light)
                    ThemeMode.DARK -> stringResource(id = R.string.theme_mode_dark)
                    ThemeMode.OLED -> stringResource(id = R.string.theme_mode_oled)
                },
                icon = Icons.Rounded.DarkMode,
                onClick = {
                    onOpenDialog(DialogSheetState.THEME_MODE_SELECTOR)
                },
            )
        }
        add { shapes ->
            PreferenceItem(
                shapes = shapes,
                title = stringResource(id = R.string.app_font_title),
                description = appFontPresetLabel(uiState.appFontPreset),
                icon = Icons.Rounded.TextFields,
                onClick = {
                    onOpenDialog(DialogSheetState.FONT_SELECTOR)
                },
            )
        }
        add { shapes ->
            PreferenceItem(
                shapes = shapes,
                title = stringResource(id = R.string.material_color_style),
                description =
                when (uiState.colorPaletteStyle) {
                    ColorPaletteStyle.TONAL_SPOT -> stringResource(id = R.string.material_color_style_tonal_spot)
                    ColorPaletteStyle.EXPRESSIVE -> stringResource(id = R.string.material_color_style_expressive)
                    ColorPaletteStyle.VIBRANT -> stringResource(id = R.string.material_color_style_vibrant)
                    ColorPaletteStyle.MONOCHROME -> stringResource(id = R.string.material_color_style_monochrome)
                },
                icon = Icons.Rounded.Palette,
                onClick = {
                    onOpenDialog(DialogSheetState.COLOR_STYLE_SELECTOR)
                },
            )
        }
        add { shapes ->
            PreferenceItem(
                shapes = shapes,
                title = stringResource(id = R.string.dynamic_color_title),
                description = if (uiState.useDynamicColor) {
                    stringResource(id = R.string.dynamic_color_description_on)
                } else {
                    stringResource(id = R.string.dynamic_color_description_off)
                },
                icon = Icons.Rounded.AutoAwesome,
                showToggle = true,
                isEnabled = true,
                isChecked = uiState.useDynamicColor,
                onClick = { settingsViewModel.setDynamicColor(!it) },
            )
        }
        add(checkAllStatusRow)
        add(aboutItem)
        add(inspectorItem)
    }

    ExpressiveSettingsSection(
        title = stringResource(id = R.string.installation),
        items = installationItems,
    )
    if (developerItems.isNotEmpty()) {
        ExpressiveSettingsSection(
            title = stringResource(id = R.string.developer_options),
            items = developerItems,
        )
    }
    ExpressiveSettingsSection(
        title = stringResource(id = R.string.other),
        items = expressiveOtherItems,
    )
}

@Composable
internal fun appFontPresetLabel(preset: AppFontPreset): String {
    return when (preset) {
        AppFontPreset.SYSTEM_DEFAULT -> stringResource(id = R.string.app_font_system_default)
        AppFontPreset.GOOGLE_SANS_FLEX -> "Google Sans Flex Rounded"
        AppFontPreset.GOOGLE_SANS_CODE -> "Google Sans Code"
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ExpressiveSettingsSection(
    title: String,
    items: List<@Composable (ListItemShapes) -> Unit>,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        items.forEachIndexed { index, item ->
            item(ListItemDefaults.segmentedShapes(index = index, count = items.size))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Menus for Dialog use (Expressive)
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun OperationModeSelectorExpressiveMenu(
    selectedMode: PreferredPrivilegedMode,
    onDismiss: () -> Unit,
    onSelectMode: (PreferredPrivilegedMode) -> Unit,
) {
    val options = listOf(
        SelectionOption(
            key = PreferredPrivilegedMode.ALL.name,
            icon = Icons.Rounded.DoneAll,
            title = stringResource(id = R.string.operation_mode_force_all),
            description = stringResource(id = R.string.operation_mode_force_all_description),
        ),
        SelectionOption(
            key = PreferredPrivilegedMode.ROOT.name,
            icon = MagiskIcon,
            title = stringResource(id = R.string.operation_mode_force_root),
            description = stringResource(id = R.string.operation_mode_force_root_description),
        ),
        SelectionOption(
            key = PreferredPrivilegedMode.SHIZUKU.name,
            icon = ShizukuIcon,
            title = stringResource(id = R.string.operation_mode_force_shizuku),
            description = stringResource(id = R.string.operation_mode_force_shizuku_description),
        ),
        SelectionOption(
            key = PreferredPrivilegedMode.DHIZUKU.name,
            icon = DhizukuIcon,
            title = stringResource(id = R.string.operation_mode_force_dhizuku),
            description = stringResource(id = R.string.operation_mode_force_dhizuku_description),
        ),
    )
    ExpressiveSelectionDialog(
        title = stringResource(id = R.string.operation_mode),
        options = options,
        selectedKey = selectedMode.name,
        onDismiss = onDismiss,
        onSelect = { selected ->
            onSelectMode(PreferredPrivilegedMode.valueOf(selected))
        },
    )
}

@Composable
internal fun ThemeModeSelectorExpressiveMenu(
    selectedMode: ThemeMode,
    onDismiss: () -> Unit,
    onSelectMode: (ThemeMode) -> Unit,
) {
    val options = listOf(
        SelectionOption(
            key = ThemeMode.SYSTEM.name,
            icon = Icons.Rounded.Settings,
            title = stringResource(id = R.string.theme_mode_system),
        ),
        SelectionOption(
            key = ThemeMode.LIGHT.name,
            icon = Icons.Rounded.LightMode,
            title = stringResource(id = R.string.theme_mode_light),
        ),
        SelectionOption(
            key = ThemeMode.DARK.name,
            icon = Icons.Rounded.DarkMode,
            title = stringResource(id = R.string.theme_mode_dark),
        ),
        SelectionOption(
            key = ThemeMode.OLED.name,
            icon = Icons.Rounded.Contrast,
            title = stringResource(id = R.string.theme_mode_oled),
        ),
    )
    ExpressiveSelectionDialog(
        title = stringResource(id = R.string.theme_mode),
        options = options,
        selectedKey = selectedMode.name,
        onDismiss = onDismiss,
        onSelect = { selected ->
            onSelectMode(ThemeMode.valueOf(selected))
        },
    )
}

@Composable
internal fun FontPresetSelectorExpressiveMenu(
    selectedPreset: AppFontPreset,
    onDismiss: () -> Unit,
    onSelectPreset: (AppFontPreset) -> Unit,
) {
    val options = listOf(
        SelectionOption(
            key = AppFontPreset.SYSTEM_DEFAULT.name,
            icon = Icons.Rounded.Settings,
            title = stringResource(id = R.string.app_font_system_default),
        ),
        SelectionOption(
            key = AppFontPreset.GOOGLE_SANS_FLEX.name,
            icon = Icons.Rounded.TextFields,
            title = "Google Sans Flex Rounded",
        ),
        SelectionOption(
            key = AppFontPreset.GOOGLE_SANS_CODE.name,
            icon = Icons.Rounded.TextFields,
            title = "Google Sans Code",
        ),
    )
    ExpressiveSelectionDialog(
        title = stringResource(id = R.string.app_font_title),
        options = options,
        selectedKey = selectedPreset.name,
        onDismiss = onDismiss,
        onSelect = { selected ->
            onSelectPreset(AppFontPreset.valueOf(selected))
        },
    )
}

@Composable
internal fun ColorStyleSelectorExpressiveMenu(
    selectedStyle: ColorPaletteStyle,
    useDynamicColor: Boolean,
    onDismiss: () -> Unit,
    onSelectStyle: (ColorPaletteStyle) -> Unit,
) {
    val styles = listOf(
        ColorPaletteStyle.TONAL_SPOT to stringResource(id = R.string.material_color_style_tonal_spot),
        ColorPaletteStyle.EXPRESSIVE to stringResource(id = R.string.material_color_style_expressive),
        ColorPaletteStyle.VIBRANT to stringResource(id = R.string.material_color_style_vibrant),
        ColorPaletteStyle.MONOCHROME to stringResource(id = R.string.material_color_style_monochrome),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.material_color_style),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                styles.forEach { (style, title) ->
                    ColorStyleSelectorRow(
                        style = style,
                        useDynamicColor = useDynamicColor,
                        title = title,
                        selected = selectedStyle == style,
                        onClick = {
                            onSelectStyle(style)
                            onDismiss()
                        },
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorStylePreview(
    style: ColorPaletteStyle,
    useDynamicColor: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Resolve the seed the same way the app theme does: wallpaper colors when
    // dynamic color is enabled (12+), the app base color otherwise.
    val wallpaperSeed = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val wallpaperColors =
                    WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                val seed =
                    wallpaperColors?.primaryColor ?: wallpaperColors?.secondaryColor ?: wallpaperColors?.tertiaryColor
                seed?.toArgb()?.let { Color(it) }
            }.getOrNull()
        } else {
            null
        }
    }
    val dynamicColorActive = useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val systemDynamicScheme =
        if (dynamicColorActive) {
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            null
        }

    // Generate the palette per style so previews differ BEFORE applying. With
    // dynamic color on, TONAL_SPOT shows the actual system dynamic scheme, exactly
    // like DsuTheme resolves it.
    val quadrants = remember(style, wallpaperSeed, isDarkTheme, dynamicColorActive, systemDynamicScheme) {
        if (dynamicColorActive && style == ColorPaletteStyle.TONAL_SPOT && systemDynamicScheme != null) {
            listOf(
                systemDynamicScheme.primary,
                systemDynamicScheme.secondary,
                systemDynamicScheme.tertiary,
                systemDynamicScheme.primaryContainer,
            )
        } else {
            val scheme = materialColorScheme(
                seedColor = wallpaperSeed ?: Primary,
                useDarkTheme = isDarkTheme,
                colorPaletteStyle = style,
            )
            listOf(
                scheme.primary,
                scheme.secondary,
                scheme.tertiary,
                scheme.primaryContainer,
            )
        }
    }

    val previewShape = when (style) {
        ColorPaletteStyle.TONAL_SPOT -> MaterialShapes.Circle.toShape()
        ColorPaletteStyle.EXPRESSIVE -> MaterialShapes.Cookie12Sided.toShape()
        ColorPaletteStyle.VIBRANT -> MaterialShapes.Clover8Leaf.toShape()
        ColorPaletteStyle.MONOCHROME -> MaterialShapes.Square.toShape()
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(previewShape),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            quadrants.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun ColorStyleSelectorRow(
    style: ColorPaletteStyle,
    useDynamicColor: Boolean,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = spring(),
        label = "colorStyleRowContainer",
    )
    val titleColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = spring(),
        label = "colorStyleRowTitle",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = containerColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ColorStylePreview(style = style, useDynamicColor = useDynamicColor)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            modifier = Modifier.weight(1f),
        )
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
internal fun ExpressiveSelectionDialog(
    title: String,
    options: List<SelectionOption>,
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    ExpressiveSelectionRow(
                        option = option,
                        selected = selectedKey == option.key,
                        onClick = {
                            onSelect(option.key)
                            onDismiss()
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.close))
            }
        },
    )
}

@Composable
private fun ExpressiveSelectionRow(
    option: SelectionOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rowScale by animateFloatAsState(
        targetValue = if (selected) 1.01f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "expressiveSelectorRowScale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = spring(),
        label = "expressiveSelectorRowContainer",
    )
    val titleColor by animateColorAsState(
        targetValue =
        if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = spring(),
        label = "expressiveSelectorRowTitle",
    )
    val descriptionColor by animateColorAsState(
        targetValue =
        if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(),
        label = "expressiveSelectorRowDescription",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clickable(onClick = onClick)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            if (option.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = descriptionColor,
                )
            }
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}
