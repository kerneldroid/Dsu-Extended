package com.dsu.extended.ui.screen.partitions

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsu.extended.R
import com.dsu.extended.ui.components.AppScaffold
import com.dsu.extended.ui.components.CardBox
import com.dsu.extended.ui.components.CardTitle
import com.dsu.extended.ui.components.DialogLikeBottomSheet
import com.dsu.extended.ui.components.ExpressiveIndeterminateLoadingBar
import com.dsu.extended.ui.components.ExpressiveProgressBar
import com.dsu.extended.ui.components.SimpleCard
import com.dsu.extended.ui.components.buttons.SecondaryButton
import com.dsu.extended.ui.screen.Destinations
import com.dsu.extended.ui.util.launcherAcResult

private const val IMAGE_MIME = "application/octet-stream"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Partitions(
    navigate: (String) -> Unit,
    partitionsViewModel: PartitionsViewModel = hiltViewModel(),
) {
    val uiState by partitionsViewModel.uiState.collectAsStateWithLifecycle()

    var pendingExport by remember { mutableStateOf<DsuImageEntry?>(null) }
    val generatedFlower = MaterialShapes.Flower.toShape()
    val flowerShape = remember { generatedFlower }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(IMAGE_MIME),
    ) { uri ->
        val entry = pendingExport
        pendingExport = null
        if (uri != null && entry != null) {
            partitionsViewModel.onExportTargetPicked(entry, uri)
        }
    }

    val pickImageLauncher = launcherAcResult { uri ->
        partitionsViewModel.onImagePickedForAdd(uri)
    }
    val pickReplacementLauncher = launcherAcResult { uri ->
        partitionsViewModel.onReplacementFilePicked(uri)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    AppScaffold(
        title = {
            Text(
                text = stringResource(id = R.string.partitions_tab_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = { navigate(Destinations.Up) }) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = { partitionsViewModel.refresh() }) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    ) {
            Column(modifier = Modifier.fillMaxSize()) {
                uiState.transfer?.let { transfer ->
                    TransferCard(
                        transfer = transfer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        onCancel = { partitionsViewModel.cancelTransfer() },
                    )
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val panel = uiState.panel) {
                        is PartitionsPanelState.Loading -> LoadingPanel()
                        is PartitionsPanelState.PrivilegedRequired -> PrivilegedRequiredPanel()
                        is PartitionsPanelState.Error ->
                            ErrorPanel(messageRes = panel.messageRes, onRetry = { partitionsViewModel.refresh() })
                        is PartitionsPanelState.Ready ->
                            ReadyPanel(
                                uiState = uiState,
                                listState = listState,
                                onExportClick = { entry ->
                                pendingExport = entry
                                exportLauncher.launch("${entry.name}.img")
                            },
                            onReplaceClick = { entry ->
                                partitionsViewModel.markReplaceTarget(entry)
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = IMAGE_MIME
                                }
                                pickReplacementLauncher.launch(intent)
                            },
                            onDeleteClick = { partitionsViewModel.showDeleteConfirmation(it) },
                        )
                    }
                    if (uiState.panel is PartitionsPanelState.Ready &&
                        uiState.transfer == null &&
                        uiState.groups.isNotEmpty() &&
                        uiState.sheet == PartitionsSheetState.NONE
                    ) {
                        val launchImagePicker = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = IMAGE_MIME
                            }
                            pickImageLauncher.launch(intent)
                        }
                        FloatingActionButton(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                                launchImagePicker()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = 20.dp,
                                    bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues()
                                        .calculateBottomPadding(),
                                ),
                            shape = flowerShape,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            // Concave outlines make Skia's spot-shadow tessellator stall the
                            // RenderThread for seconds per frame; draw the flower flat.
                            // Every state must be zeroed explicitly: pressed/focused/hovered
                            // default to their own tokens, not to defaultElevation.
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp,
                            ),
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                        }
                    }
                }
            }
    }

    when (uiState.sheet) {
        PartitionsSheetState.ADD_IMAGE -> AddImageSheet(uiState, partitionsViewModel)
        PartitionsSheetState.CONFIRM_REPLACE -> ReplaceSheet(uiState, partitionsViewModel)
        PartitionsSheetState.CONFIRM_DELETE -> DeleteSheet(uiState, partitionsViewModel)
        PartitionsSheetState.NONE -> {}
    }
}

@Composable
private fun LoadingPanel() {
    SimpleCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        cardTitle = stringResource(id = R.string.partitions_tab_title),
    ) {
        ExpressiveIndeterminateLoadingBar(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            progressColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun PrivilegedRequiredPanel() {
    SimpleCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        cardTitle = stringResource(id = R.string.partitions_privileged_title),
        text = stringResource(id = R.string.partitions_privileged_description),
        cardColor = MaterialTheme.colorScheme.errorContainer,
    )
}

@Composable
private fun ErrorPanel(messageRes: Int, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SimpleCard(cardTitle = stringResource(id = messageRes))
        SecondaryButton(text = stringResource(id = R.string.partitions_retry), onClick = onRetry)
    }
}

@Composable
private fun ReadyPanel(
    uiState: PartitionsUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onExportClick: (DsuImageEntry) -> Unit,
    onReplaceClick: (DsuImageEntry) -> Unit,
    onDeleteClick: (DsuImageEntry) -> Unit,
) {
    if (uiState.groups.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.partitions_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 10.dp,
            bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        uiState.groups.forEach { group ->
            item(key = "title_${group.prefix}") {
                CardBox(modifier = Modifier.fillMaxWidth()) {
                    CardTitle(cardTitle = group.prefix.trim('/'))
                }
            }
            items(count = group.images.size, key = { index -> "${group.prefix}/${group.images[index].name}" }) { index ->
                ImageRow(
                    entry = group.images[index],
                    enabled = uiState.transfer == null && uiState.sheet == PartitionsSheetState.NONE,
                    onExportClick = onExportClick,
                    onReplaceClick = onReplaceClick,
                    onDeleteClick = onDeleteClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageRow(
    entry: DsuImageEntry,
    enabled: Boolean,
    onExportClick: (DsuImageEntry) -> Unit,
    onReplaceClick: (DsuImageEntry) -> Unit,
    onDeleteClick: (DsuImageEntry) -> Unit,
) {
    val replaceLabel = stringResource(id = R.string.partitions_action_replace)
    val exportLabel = stringResource(id = R.string.partitions_action_export)
    val deleteLabel = stringResource(id = R.string.partitions_action_delete)
    CardBox(
        modifier = Modifier.fillMaxWidth(),
        addPadding = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.partitions_image_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            ButtonGroup(
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState, enabled = enabled)
                },
            ) {
                clickableItem(
                    onClick = { onReplaceClick(entry) },
                    label = replaceLabel,
                    icon = { Icon(Icons.Rounded.SwapVert, contentDescription = null) },
                    enabled = enabled,
                )
                clickableItem(
                    onClick = { onExportClick(entry) },
                    label = exportLabel,
                    icon = { Icon(Icons.Rounded.SaveAs, contentDescription = null) },
                    enabled = enabled,
                )
                clickableItem(
                    onClick = { onDeleteClick(entry) },
                    label = deleteLabel,
                    icon = {
                        Icon(
                            Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun TransferCard(
    transfer: PartitionTransferState,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
) {
    CardBox(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(
                id = when (transfer.kind) {
                    PartitionTransferKind.ADD -> R.string.partitions_progress_adding
                    PartitionTransferKind.REPLACE -> R.string.partitions_progress_replacing
                    PartitionTransferKind.EXPORT -> R.string.partitions_progress_exporting
                },
                transfer.imageName,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        if (transfer.isIndeterminate) {
            ExpressiveIndeterminateLoadingBar(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                progressColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            )
        } else {
            ExpressiveProgressBar(
                progress = transfer.copiedBytes.toFloat() / transfer.totalBytes.toFloat(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                progressColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            )
            Text(
                text = stringResource(
                    id = R.string.partitions_progress_bytes,
                    formatGb(transfer.copiedBytes),
                    formatGb(transfer.totalBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text(stringResource(id = R.string.partitions_cancel_transfer))
            }
        }
    }
}

private fun formatGb(bytes: Long): String {
    return String.format(Locale.US, "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

@Composable
private fun AddImageSheet(
    uiState: PartitionsUiState,
    viewModel: PartitionsViewModel,
) {
    var readOnly by rememberSaveable { mutableStateOf(true) }
    DialogLikeBottomSheet(
        icon = Icons.Rounded.Add,
        title = stringResource(id = R.string.partitions_add_title),
        text = stringResource(id = R.string.partitions_add_description, uiState.pendingAdd?.prefix?.trim('/') ?: ""),
        confirmText = stringResource(id = R.string.partitions_confirm),
        cancelText = stringResource(id = R.string.partitions_cancel),
        onClickConfirm = { viewModel.confirmAddImage(readOnly) },
        onClickCancel = { viewModel.dismissSheet() },
        content = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(id = R.string.partitions_readonly))
                Switch(checked = readOnly, onCheckedChange = { readOnly = it })
            }
        },
    )
}

@Composable
private fun ReplaceSheet(
    uiState: PartitionsUiState,
    viewModel: PartitionsViewModel,
) {
    DialogLikeBottomSheet(
        icon = Icons.Rounded.SwapVert,
        title = stringResource(id = R.string.partitions_replace_title),
        text = stringResource(id = R.string.partitions_replace_description, uiState.pendingReplace?.entry?.name ?: ""),
        confirmText = stringResource(id = R.string.partitions_confirm),
        cancelText = stringResource(id = R.string.partitions_cancel),
        onClickConfirm = { viewModel.confirmReplaceImage() },
        onClickCancel = { viewModel.dismissSheet() },
    )
}

@Composable
private fun DeleteSheet(
    uiState: PartitionsUiState,
    viewModel: PartitionsViewModel,
) {
    DialogLikeBottomSheet(
        icon = Icons.Rounded.DeleteForever,
        title = stringResource(id = R.string.partitions_delete_title),
        text = stringResource(id = R.string.partitions_delete_description, uiState.pendingDelete?.name ?: ""),
        confirmText = stringResource(id = R.string.partitions_delete_confirm),
        cancelText = stringResource(id = R.string.partitions_cancel),
        onClickConfirm = { viewModel.confirmDeleteImage() },
        onClickCancel = { viewModel.dismissSheet() },
    )
}
