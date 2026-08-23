package com.dsu.extended.ui.cards

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dsu.extended.R
import com.dsu.extended.ui.components.SimpleCard
import com.dsu.extended.ui.components.buttons.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun CopyableTextCard(
    text: String,
    showToast: Boolean = true,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val copiedText = stringResource(id = R.string.copied)

    SimpleCard(
        text = text,
        content = {
            Row {
                Spacer(modifier = Modifier.weight(1F))
                PrimaryButton(
                    text = stringResource(id = R.string.copy_text),
                    onClick = {
                        coroutineScope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("text", text)))
                        }
                        if (showToast) {
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        },
    )
}
