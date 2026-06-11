package com.dsu.extended.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dsu.extended.R
import com.dsu.extended.ui.components.SimpleCard
import com.dsu.extended.ui.components.buttons.SecondaryButton

@Composable
fun DsuInfoCard(
    modifier: Modifier = Modifier,
    onClickViewDocs: () -> Unit,
    onClickLearnMore: () -> Unit,
) {
    SimpleCard(
        modifier = modifier,
        cardTitle = stringResource(id = R.string.what_is_dsu),
        text = stringResource(id = R.string.what_is_dsu_description),
        justifyText = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.view_docs),
                onClick = onClickViewDocs,
            )
            SecondaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.learn_more),
                onClick = onClickLearnMore,
            )
        }
    }
}
