package com.dsu.extended.ui.sdialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dsu.extended.R
import com.dsu.extended.ui.components.DialogLikeBottomSheet

@Composable
fun DiscardDSUSheet(
    onClickConfirm: () -> Unit,
    onClickCancel: () -> Unit,
) {
    DialogLikeBottomSheet(
        title = stringResource(id = R.string.discard_dsu_question),
        icon = Icons.Rounded.DeleteForever,
        text = stringResource(id = R.string.dsu_already_installed_warning),
        confirmText = stringResource(id = R.string.yes),
        cancelText = stringResource(id = R.string.cancel),
        onClickConfirm = onClickConfirm,
        onClickCancel = onClickCancel,
    )
}
