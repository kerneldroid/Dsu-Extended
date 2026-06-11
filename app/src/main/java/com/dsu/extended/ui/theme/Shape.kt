package com.dsu.extended.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive Shape System
 * Larger, more pronounced corner radii for modern look
 */
val ExpressiveShapes = Shapes(
    // For small components like chips, small buttons
    extraSmall = RoundedCornerShape(12.dp),

    // For input fields, small cards
    small = RoundedCornerShape(16.dp),

    // For medium cards, dialogs
    medium = RoundedCornerShape(24.dp),

    // For large cards, bottom sheets
    large = RoundedCornerShape(32.dp),

    // For full-height elements, large containers
    extraLarge = RoundedCornerShape(40.dp),
)

/**
 * Custom shape definitions for specific components
 */
object DSUShapes {
    // Main content cards
    val CardShape = RoundedCornerShape(28.dp)

    // Primary action buttons
    val ButtonShape = RoundedCornerShape(20.dp)

    // FAB and circular buttons
    val FabShape = RoundedCornerShape(24.dp)

    // Bottom sheet
    val BottomSheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp,
    )

    // Dialog
    val DialogShape = RoundedCornerShape(32.dp)

    // Input fields
    val InputShape = RoundedCornerShape(16.dp)

    // Chips and tags
    val ChipShape = RoundedCornerShape(12.dp)

    // Progress bar
    val ProgressShape = RoundedCornerShape(10.dp)

    // Snackbar
    val SnackbarShape = RoundedCornerShape(16.dp)

    // Image containers
    val ImageShape = RoundedCornerShape(20.dp)

    // Icon buttons
    val IconButtonShape = RoundedCornerShape(16.dp)

    // Toggle/Switch track
    val ToggleShape = RoundedCornerShape(50)

    // Top bar (when scrolled)
    val TopBarShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp,
    )

    // Status indicators
    val StatusShape = RoundedCornerShape(8.dp)

    // Installation progress card (larger)
    val InstallationCardShape = RoundedCornerShape(36.dp)
}
