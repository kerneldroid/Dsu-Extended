package com.dsu.extended.ui.theme

import android.app.Activity
import android.app.WallpaperManager
import android.view.Window
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    OLED;

    val value: String
        get() = name.lowercase()

    companion object {
        fun fromPreference(value: String): ThemeMode {
            return entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }
}

enum class ColorPaletteStyle {
    TONAL_SPOT,
    NEUTRAL,
    VIBRANT,
    EXPRESSIVE,
    RAINBOW,
    FRUIT_SALAD,
    MONOCHROME,
    FIDELITY,
    CONTENT;

    val value: String
        get() = name.lowercase()

    /** Styles the Material 3 2025 color spec supports; others fall back to 2021. */
    val supportsSpec2025: Boolean
        get() = this == TONAL_SPOT || this == NEUTRAL || this == VIBRANT || this == EXPRESSIVE

    companion object {
        fun fromPreference(value: String): ColorPaletteStyle {
            return entries.firstOrNull { it.value == value } ?: TONAL_SPOT
        }
    }
}

enum class ColorSpecVersion {
    SPEC_2021,
    SPEC_2025;

    val value: String
        get() = name.lowercase()

    companion object {
        fun fromPreference(value: String): ColorSpecVersion {
            return entries.firstOrNull { it.value == value } ?: SPEC_2021
        }
    }
}

internal fun ColorScheme.toOledSurfaceScheme(): ColorScheme {
    return copy(
        background = Color(0xFF000000),
        onBackground = Color(0xFFF2F2F2),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFF2F2F2),
        surfaceVariant = Color(0xFF141414),
        onSurfaceVariant = Color(0xFFE7E7E7),
        inverseSurface = Color(0xFFEAEAEA),
        inverseOnSurface = Color(0xFF111111),
        outline = Color(0xFF4A4A4A),
        outlineVariant = Color(0xFF222222),
        scrim = Color(0xD9000000),
        surfaceBright = Color(0xFF000000),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF000000),
        surfaceContainer = Color(0xFF000000),
        surfaceContainerHigh = Color(0xFF0A0A0A),
        surfaceContainerHighest = Color(0xFF141414),
    )
}

internal fun materialColorScheme(
    seedColor: Color,
    useDarkTheme: Boolean,
    colorPaletteStyle: ColorPaletteStyle,
    colorSpecVersion: ColorSpecVersion = ColorSpecVersion.SPEC_2021,
    specVersion: ColorSpecVersion = ColorSpecVersion.SPEC_2021,
): ColorScheme {
    val style = when (colorPaletteStyle) {
        ColorPaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
        ColorPaletteStyle.NEUTRAL -> PaletteStyle.Neutral
        ColorPaletteStyle.VIBRANT -> PaletteStyle.Vibrant
        ColorPaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
        ColorPaletteStyle.RAINBOW -> PaletteStyle.Rainbow
        ColorPaletteStyle.FRUIT_SALAD -> PaletteStyle.FruitSalad
        ColorPaletteStyle.MONOCHROME -> PaletteStyle.Monochrome
        ColorPaletteStyle.FIDELITY -> PaletteStyle.Fidelity
        ColorPaletteStyle.CONTENT -> PaletteStyle.Content
    }
    // The 2025 spec only covers a subset of styles; the rest fall back to 2021.
    val spec = when (specVersion) {
        ColorSpecVersion.SPEC_2025 ->
            if (colorPaletteStyle.supportsSpec2025) ColorSpec.SpecVersion.SPEC_2025 else ColorSpec.SpecVersion.SPEC_2021
        ColorSpecVersion.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
    }
    return dynamicColorScheme(
        seedColor = seedColor,
        isDark = useDarkTheme,
        style = style,
        contrastLevel = 0.0,
        specVersion = spec,
    )
}


@Composable
private fun animatedColorScheme(colorScheme: ColorScheme): ColorScheme {
    val animSpec =
        spring<Color>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    val primary by animateColorAsState(colorScheme.primary, animSpec, label = "primary")
    val onPrimary by animateColorAsState(colorScheme.onPrimary, animSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(colorScheme.primaryContainer, animSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(colorScheme.onPrimaryContainer, animSpec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(colorScheme.secondary, animSpec, label = "secondary")
    val onSecondary by animateColorAsState(colorScheme.onSecondary, animSpec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(colorScheme.secondaryContainer, animSpec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(colorScheme.onSecondaryContainer, animSpec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(colorScheme.tertiary, animSpec, label = "tertiary")
    val onTertiary by animateColorAsState(colorScheme.onTertiary, animSpec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(colorScheme.tertiaryContainer, animSpec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(colorScheme.onTertiaryContainer, animSpec, label = "onTertiaryContainer")
    val background by animateColorAsState(colorScheme.background, animSpec, label = "background")
    val onBackground by animateColorAsState(colorScheme.onBackground, animSpec, label = "onBackground")
    val surface by animateColorAsState(colorScheme.surface, animSpec, label = "surface")
    val onSurface by animateColorAsState(colorScheme.onSurface, animSpec, label = "onSurface")
    val surfaceVariant by animateColorAsState(colorScheme.surfaceVariant, animSpec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(colorScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant")
    val error by animateColorAsState(colorScheme.error, animSpec, label = "error")
    val onError by animateColorAsState(colorScheme.onError, animSpec, label = "onError")
    val errorContainer by animateColorAsState(colorScheme.errorContainer, animSpec, label = "errorContainer")
    val onErrorContainer by animateColorAsState(colorScheme.onErrorContainer, animSpec, label = "onErrorContainer")
    val outline by animateColorAsState(colorScheme.outline, animSpec, label = "outline")
    val outlineVariant by animateColorAsState(colorScheme.outlineVariant, animSpec, label = "outlineVariant")

    return colorScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DsuExtendedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorPaletteStyle: ColorPaletteStyle = ColorPaletteStyle.TONAL_SPOT,
    colorSpecVersion: ColorSpecVersion = ColorSpecVersion.SPEC_2021,
    appFontPreset: AppFontPreset = AppFontPreset.GOOGLE_SANS_FLEX,
    animateColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val useDarkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> darkTheme
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.OLED -> true
        }
    val effectiveDynamicColor = dynamicColor
    val effectivePaletteStyle = colorPaletteStyle
    val motionScheme = MotionScheme.expressive()
    val selectedFontFamily = resolveAppFontFamily(appFontPreset)
    val typography = createExpressiveTypography(appFontPreset)
    val dsuTextStyles = remember(selectedFontFamily) { createDsuTextStyles(selectedFontFamily) }
    val shapes = ExpressiveShapes

    val context = LocalContext.current
    val dynamicSystemColorScheme =
        if (effectiveDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            null
        }
    val dynamicSeedColor =
        if (effectiveDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val wallpaperColors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                val wallpaperSeed =
                    wallpaperColors?.primaryColor ?: wallpaperColors?.secondaryColor ?: wallpaperColors?.tertiaryColor
                wallpaperSeed?.toArgb()?.let { Color(it) }
            }.getOrNull() ?: dynamicSystemColorScheme?.primary
        } else {
            null
        }
    val materialBaseColorScheme =
        if (
            effectiveDynamicColor &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            effectivePaletteStyle == ColorPaletteStyle.TONAL_SPOT &&
            dynamicSystemColorScheme != null
        ) {
            dynamicSystemColorScheme
        } else {
            materialColorScheme(
                seedColor = dynamicSeedColor ?: Primary,
                useDarkTheme = useDarkTheme,
                colorPaletteStyle = effectivePaletteStyle,
                specVersion = colorSpecVersion,
            )
        }
    val normalizedColorScheme =
        if (themeMode == ThemeMode.OLED) {
            materialBaseColorScheme.toOledSurfaceScheme()
        } else {
            materialBaseColorScheme
        }
    val colorScheme =
        if (animateColors) {
            animatedColorScheme(normalizedColorScheme)
        } else {
            normalizedColorScheme
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                val statusBarArgb =
                    if (themeMode == ThemeMode.OLED) {
                        Color(0xFF000000)
                    } else {
                        Color.Transparent
                    }
                val navigationBarArgb =
                    if (themeMode == ThemeMode.OLED) {
                        Color(0xFF000000).toArgb()
                    } else if (useDarkTheme) {
                        Color(0x70000000).toArgb()
                    } else {
                        Color(0x70FFFFFF).toArgb()
                    }
                applyLegacySystemBarColors(window, statusBarArgb.toArgb(), navigationBarArgb)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    @Composable
    fun ApplyThemeContent() {
        CompositionLocalProvider(
            LocalDsuTextStyles provides dsuTextStyles,
        ) {
            MaterialExpressiveTheme(
                colorScheme = colorScheme,
                motionScheme = motionScheme,
                typography = typography,
                shapes = shapes,
                content = content,
            )
        }
    }

    ApplyThemeContent()
}

object SemanticColors {
    @Composable
    fun success(): Color = if (isSystemInDarkTheme()) SuccessDark else Success

    @Composable
    fun successContainer(): Color = if (isSystemInDarkTheme()) SuccessContainerDark else SuccessContainer

    @Composable
    fun warning(): Color = if (isSystemInDarkTheme()) WarningDark else Warning

    @Composable
    fun warningContainer(): Color = if (isSystemInDarkTheme()) WarningContainerDark else WarningContainer

    @Composable
    fun info(): Color = if (isSystemInDarkTheme()) InfoDark else Info

    @Composable
    fun infoContainer(): Color = if (isSystemInDarkTheme()) InfoContainerDark else InfoContainer

    @Composable
    fun glassOverlay(): Color = if (isSystemInDarkTheme()) GlassDark else GlassLight
}

// Colored system bars still work below API 35; edge-to-edge takes over afterwards.
@Suppress("DEPRECATION")
private fun applyLegacySystemBarColors(window: Window, statusBarArgb: Int, navigationBarArgb: Int) {
    window.statusBarColor = statusBarArgb
    window.navigationBarColor = navigationBarArgb
}

