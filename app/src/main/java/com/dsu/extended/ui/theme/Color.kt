package com.dsu.extended.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// Material 3 Expressive Color Palette
// Modern, vibrant colors with dynamic contrast
// ═══════════════════════════════════════════════════════════════

// Primary - Vibrant Electric Blue
val Primary = Color(0xFF0066FF)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFD6E3FF)
val OnPrimaryContainer = Color(0xFF001A41)
val PrimaryDark = Color(0xFFADC6FF)
val OnPrimaryDark = Color(0xFF002E69)
val PrimaryContainerDark = Color(0xFF004494)
val OnPrimaryContainerDark = Color(0xFFD6E3FF)

// Secondary - Expressive Teal
val Secondary = Color(0xFF00BFA5)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFB2FFF0)
val OnSecondaryContainer = Color(0xFF002019)
val SecondaryDark = Color(0xFF4FDBC7)
val OnSecondaryDark = Color(0xFF00382D)
val SecondaryContainerDark = Color(0xFF005142)
val OnSecondaryContainerDark = Color(0xFFB2FFF0)

// Tertiary - Warm Orange Accent
val Tertiary = Color(0xFFFF6D00)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFDBCF)
val OnTertiaryContainer = Color(0xFF341100)
val TertiaryDark = Color(0xFFFFB599)
val OnTertiaryDark = Color(0xFF552100)
val TertiaryContainerDark = Color(0xFF793200)
val OnTertiaryContainerDark = Color(0xFFFFDBCF)

// Error - Material Red
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Neutral / Surface - Light Mode
val Background = Color(0xFFFDFCFF)
val OnBackground = Color(0xFF1A1C1E)
val Surface = Color(0xFFFDFCFF)
val OnSurface = Color(0xFF1A1C1E)
val SurfaceVariant = Color(0xFFE0E2EC)
val OnSurfaceVariant = Color(0xFF44474E)
val Outline = Color(0xFF74777F)
val OutlineVariant = Color(0xFFC4C6D0)

// Neutral / Surface - Dark Mode
val BackgroundDark = Color(0xFF1A1C1E)
val OnBackgroundDark = Color(0xFFE3E2E6)
val SurfaceDark = Color(0xFF1A1C1E)
val OnSurfaceDark = Color(0xFFE3E2E6)
val SurfaceVariantDark = Color(0xFF44474E)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)
val OutlineDark = Color(0xFF8E9099)
val OutlineVariantDark = Color(0xFF44474E)

// OLED Black Theme
val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF0A0A0A)
val OledSurfaceVariant = Color(0xFF1A1A1A)

// Surface Containers (Material 3.1)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF7F8FC)
val SurfaceContainer = Color(0xFFF1F2F6)
val SurfaceContainerHigh = Color(0xFFEBECF0)
val SurfaceContainerHighest = Color(0xFFE5E6EA)

val SurfaceContainerLowestDark = Color(0xFF0F1113)
val SurfaceContainerLowDark = Color(0xFF1A1C1E)
val SurfaceContainerDark = Color(0xFF1E2022)
val SurfaceContainerHighDark = Color(0xFF292A2D)
val SurfaceContainerHighestDark = Color(0xFF333538)

// Glassmorphism / Overlay
val GlassLight = Color(0xE6FFFFFF)
val GlassDark = Color(0xE61C1C1E)
val GlassOled = Color(0xCC0A0A0A)
val ScrimColor = Color(0x80000000)

// Semantic Colors - Success
val Success = Color(0xFF00C853)
val OnSuccess = Color(0xFFFFFFFF)
val SuccessContainer = Color(0xFFC8FFC8)
val OnSuccessContainer = Color(0xFF002200)
val SuccessDark = Color(0xFF5AE06A)
val SuccessContainerDark = Color(0xFF005300)

// Semantic Colors - Warning
val Warning = Color(0xFFFFAB00)
val OnWarning = Color(0xFF000000)
val WarningContainer = Color(0xFFFFE082)
val OnWarningContainer = Color(0xFF261A00)
val WarningDark = Color(0xFFFFD54F)
val WarningContainerDark = Color(0xFF614D00)

// Semantic Colors - Info
val Info = Color(0xFF2196F3)
val OnInfo = Color(0xFFFFFFFF)
val InfoContainer = Color(0xFFBBDEFB)
val OnInfoContainer = Color(0xFF001E3C)
val InfoDark = Color(0xFF64B5F6)
val InfoContainerDark = Color(0xFF004A8F)

// Installation Progress Colors
val ProgressActive = Color(0xFF00BFA5)
val ProgressTrack = Color(0xFFE0E0E0)
val ProgressTrackDark = Color(0xFF333538)

// Card Colors with Gradients
object GradientColors {
    val PrimaryGradient = listOf(
        Color(0xFF0066FF),
        Color(0xFF00AAFF),
    )

    val SecondaryGradient = listOf(
        Color(0xFF00BFA5),
        Color(0xFF00E5CC),
    )

    val SuccessGradient = listOf(
        Color(0xFF00C853),
        Color(0xFF69F0AE),
    )

    val ErrorGradient = listOf(
        Color(0xFFFF5252),
        Color(0xFFFF8A80),
    )

    val WarningGradient = listOf(
        Color(0xFFFF9800),
        Color(0xFFFFCC80),
    )
}

// Legacy colors for backward compatibility
val Blue80 = PrimaryDark
val BlueGrey80 = SecondaryDark
val Purplish80 = TertiaryDark

val Blue40 = Primary
val BlueGrey40 = Secondary
val Purplish40 = Tertiary
