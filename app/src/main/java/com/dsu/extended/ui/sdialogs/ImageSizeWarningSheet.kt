package com.dsu.extended.ui.sdialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dsu.extended.R
import com.dsu.extended.ui.components.DialogLikeBottomSheet

@Composable
fun ImageSizeWarningSheet(
    onClickConfirm: () -> Unit,
    onClickCancel: () -> Unit,
) {
    DialogLikeBottomSheet(
        title = stringResource(id = R.string.dialog_image_size),
        icon = Icons.Rounded.Edit,
        text = stringResource(id = R.string.dialog_image_size_description),
        confirmText = stringResource(id = R.string.set_anyway),
        cancelText = stringResource(id = R.string.cancel),
        confirmGlow = true,
        onClickConfirm = onClickConfirm,
        onClickCancel = onClickCancel,
    )
}
