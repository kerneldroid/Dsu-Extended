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
    // Main content cards (reduced from 28.dp for a compact look)
    val CardShape = RoundedCornerShape(18.dp)

    // Chips and tags
    val ChipShape = RoundedCornerShape(10.dp)
}
