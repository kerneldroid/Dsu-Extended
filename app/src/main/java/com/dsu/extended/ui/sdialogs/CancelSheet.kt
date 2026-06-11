package com.dsu.extended.ui.sdialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dsu.extended.R
import com.dsu.extended.ui.components.DialogLikeBottomSheet

@Composable
fun CancelSheet(
    onClickConfirm: () -> Unit,
    onClickCancel: () -> Unit,
) {
    DialogLikeBottomSheet(
        title = stringResource(id = R.string.cancel_installation_question),
        icon = Icons.Rounded.Cancel,
        text = stringResource(id = R.string.cancel_installation_description),
        confirmText = stringResource(id = R.string.yes),
        cancelText = stringResource(id = R.string.no),
        onClickConfirm = onClickConfirm,
        onClickCancel = onClickCancel,
    )
}
