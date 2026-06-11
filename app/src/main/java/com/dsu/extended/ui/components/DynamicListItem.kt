package com.dsu.extended.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

@Composable
fun DynamicListItem(
    listLength: Int,
    currentValue: Int,
    content: @Composable () -> Unit,
) {
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val safeListLength = if (listLength < 0) 0 else listLength
    val safeCurrentValue = currentValue.coerceIn(0, safeListLength)
    val safeShape = when (safeCurrentValue) {
        0 -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        safeListLength -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
        else -> RoundedCornerShape(8.dp)
    }
    CardBox(
        addPadding = false,
        roundedCornerShape = safeShape,
        cardColor = cardColor,
    ) {
        content()
    }
}
