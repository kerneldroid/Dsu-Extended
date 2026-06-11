package com.dsu.extended.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.dsu.extended.ui.theme.DSUTextStyles

@Composable
fun CardTitle(
    modifier: Modifier = Modifier,
    cardTitle: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        modifier = modifier,
        text = cardTitle,
        style = DSUTextStyles.cardTitle,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}
