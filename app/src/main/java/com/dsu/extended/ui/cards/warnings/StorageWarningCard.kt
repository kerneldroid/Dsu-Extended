package com.dsu.extended.ui.cards.warnings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dsu.extended.R
import com.dsu.extended.ui.components.SimpleCard
import com.dsu.extended.ui.components.buttons.PrimaryButton

@Composable
fun StorageWarningCard(
    minPercentageFreeStorage: String,
    onClick: () -> Unit,
) {
    SimpleCard(
        modifier = Modifier.fillMaxWidth(),
        cardTitle = stringResource(id = R.string.storage_warning),
        text = stringResource(id = R.string.storage_warning_description, minPercentageFreeStorage),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            PrimaryButton(text = stringResource(id = R.string.continue_anyway), onClick = onClick)
        }
    }
}
