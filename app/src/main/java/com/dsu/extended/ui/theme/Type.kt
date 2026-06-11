package com.dsu.extended.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dsu.extended.R

/**
 * Material 3 Expressive Typography system.
 * Google Sans Flex Variable Font integration.
 */

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("ROND", 100f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val GoogleSansCodeFamily = FontFamily(
    Font(
        resId = R.font.google_sans_code,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.Setting("MONO", 1f)
        )
    )
)

enum class AppFontPreset {
    SYSTEM_DEFAULT,
    GOOGLE_SANS_FLEX,
    GOOGLE_SANS_CODE;

    val value: String
        get() = name.lowercase()

    companion object {
        fun fromPreference(value: String): AppFontPreset {
            return entries.firstOrNull { it.value == value } ?: GOOGLE_SANS_FLEX
        }
    }
}

@OptIn(ExperimentalTextApi::class)
fun resolveAppFontFamily(preset: AppFontPreset): FontFamily {
    return when (preset) {
        AppFontPreset.SYSTEM_DEFAULT -> FontFamily.Default
        AppFontPreset.GOOGLE_SANS_FLEX -> GoogleSansFlexFamily
        AppFontPreset.GOOGLE_SANS_CODE -> GoogleSansCodeFamily
    }
}

@OptIn(ExperimentalTextApi::class)
fun createExpressiveTypography(preset: AppFontPreset): Typography {
    if (preset == AppFontPreset.SYSTEM_DEFAULT) {
        return createSystemTypography()
    }

    val displayFont: FontFamily
    val headlineFont: FontFamily
    val bodyFont: FontFamily

    if (preset == AppFontPreset.GOOGLE_SANS_CODE) {
        val codeSettings = FontVariation.Settings(
            FontVariation.weight(450),
            FontVariation.Setting("MONO", 1f)
        )
        val codeHeadlineSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.Setting("MONO", 1f)
        )
        displayFont = FontFamily(Font(resId = R.font.google_sans_code, variationSettings = codeHeadlineSettings))
        headlineFont = FontFamily(Font(resId = R.font.google_sans_code, variationSettings = codeHeadlineSettings))
        bodyFont = FontFamily(Font(resId = R.font.google_sans_code, variationSettings = codeSettings))
    } else {
        // Zenith Preset Axes (Google Sans Flex)
        val displaySettings = FontVariation.Settings(
            FontVariation.weight(950),
            FontVariation.width(85f),
            FontVariation.Setting("opsz", 30f),
            FontVariation.grade(0),
            FontVariation.slant(0f),
            FontVariation.Setting("ROND", 100f)
        )
        val headlineSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(115f),
            FontVariation.Setting("opsz", 32f),
            FontVariation.grade(0),
            FontVariation.slant(0f),
            FontVariation.Setting("ROND", 60f)
        )
        val bodySettings = FontVariation.Settings(
            FontVariation.weight(450),
            FontVariation.width(100f),
            FontVariation.Setting("opsz", 16f),
            FontVariation.grade(20),
            FontVariation.slant(0f),
            FontVariation.Setting("ROND", 0f)
        )

        displayFont = FontFamily(Font(resId = R.font.google_sans_flex_variable, variationSettings = displaySettings))
        headlineFont = FontFamily(Font(resId = R.font.google_sans_flex_variable, variationSettings = headlineSettings))
        bodyFont = FontFamily(Font(resId = R.font.google_sans_flex_variable, variationSettings = bodySettings))
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),

        headlineLarge = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),

        titleLarge = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = headlineFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),

        bodyLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),

        labelLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

fun createSystemTypography(): Typography {
    val fontFamily = FontFamily.Default
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),

        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),

        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),

        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),

        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

val AppFontFamily = GoogleSansFlexFamily
val Typography = createExpressiveTypography(AppFontPreset.GOOGLE_SANS_FLEX)

@Immutable
data class DsuTextStyleSet(
    val cardTitle: TextStyle,
    val progressText: TextStyle,
    val statusText: TextStyle,
    val logText: TextStyle,
    val errorText: TextStyle,
    val suggestionText: TextStyle,
    val deviceInfoText: TextStyle,
    val buttonText: TextStyle,
)

fun createDsuTextStyles(fontFamily: FontFamily): DsuTextStyleSet {
    return DsuTextStyleSet(
        cardTitle = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        ),
        progressText = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp,
        ),
        statusText = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        logText = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
        ),
        errorText = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        suggestionText = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.25.sp,
        ),
        deviceInfoText = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        buttonText = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
    )
}

val LocalDsuTextStyles = staticCompositionLocalOf { createDsuTextStyles(AppFontFamily) }

object DSUTextStyles {
    val cardTitle: TextStyle
        @Composable get() = LocalDsuTextStyles.current.cardTitle

    val progressText: TextStyle
        @Composable get() = LocalDsuTextStyles.current.progressText

    val statusText: TextStyle
        @Composable get() = LocalDsuTextStyles.current.statusText

    val logText: TextStyle
        @Composable get() = LocalDsuTextStyles.current.logText

    val errorText: TextStyle
        @Composable get() = LocalDsuTextStyles.current.errorText

    val suggestionText: TextStyle
        @Composable get() = LocalDsuTextStyles.current.suggestionText

    val deviceInfoText: TextStyle
        @Composable get() = LocalDsuTextStyles.current.deviceInfoText

    val buttonText: TextStyle
        @Composable get() = LocalDsuTextStyles.current.buttonText
}
