package com.dsu.extended.ui.cards.warnings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dsu.extended.R
import com.dsu.extended.ui.components.ExpressiveIndeterminateLoadingBar
import com.dsu.extended.ui.components.SimpleCard

@Composable
fun GrantingPermissionCard() {
    val progressColor = MaterialTheme.colorScheme.primary
    val progressTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    SimpleCard(
        modifier = Modifier.fillMaxWidth(),
        cardTitle = stringResource(id = R.string.missing_permission),
        text = stringResource(id = R.string.granting_permission),
    ) {
        ExpressiveIndeterminateLoadingBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            progressColor = progressColor,
            trackColor = progressTrackColor,
        )
    }
}
