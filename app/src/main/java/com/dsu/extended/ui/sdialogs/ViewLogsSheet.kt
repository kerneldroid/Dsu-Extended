package com.dsu.extended.ui.sdialogs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dsu.extended.R
import com.dsu.extended.ui.cards.LogcatCard
import com.dsu.extended.ui.components.CustomBottomSheet
import com.dsu.extended.ui.components.buttons.PrimaryButton
import com.dsu.extended.ui.util.launcherAcResult

@Composable
fun ViewLogsBottomSheet(
    logs: String,
    onClickSaveLogs: (Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val logsSavedText = stringResource(id = R.string.saved_logs)

    val saveLogsResult = launcherAcResult {
        onClickSaveLogs(it)
        Toast.makeText(context, logsSavedText, Toast.LENGTH_SHORT).show()
    }

    CustomBottomSheet(
        title = stringResource(id = R.string.installation_logs),
        icon = Icons.Rounded.Description,
        onDismiss = onDismiss,
    ) {
        LogcatCard(logs = logs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Spacer(modifier = Modifier.weight(1F))
            PrimaryButton(
                text = stringResource(id = R.string.save_logs),
                onClick = {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "application/zip"
                    intent.putExtra(Intent.EXTRA_TITLE, "dsu-logs.zip")
                    saveLogsResult.launch(intent)
                },
            )
        }
    }
}
