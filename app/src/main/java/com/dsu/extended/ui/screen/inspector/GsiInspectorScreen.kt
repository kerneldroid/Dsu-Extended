package com.dsu.extended.ui.screen.inspector

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsu.extended.R
import com.dsu.extended.model.DsuSystemMetadata
import com.dsu.extended.ui.components.AppScaffold
import com.dsu.extended.ui.components.PreferenceItem
import com.dsu.extended.ui.components.buttons.SecondaryButton
import androidx.compose.foundation.layout.Box
import com.dsu.extended.ui.components.CardBox
import com.dsu.extended.ui.components.DialogLikeBottomSheet
import com.dsu.extended.ui.components.ExpressiveIndeterminateLoadingBar
import com.dsu.extended.ui.components.ExpressiveProgressBar
import com.dsu.extended.ui.components.SimpleCard
import com.dsu.extended.ui.screen.Destinations
import java.util.Locale

private const val IMAGE_MIME = "application/octet-stream"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GsiInspectorScreen(
    navigate: (String) -> Unit,
    viewModel: GsiInspectorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(IMAGE_MIME),
    ) { uri ->
        val relativePath = uiState.export?.relativePath
        if (uri != null && relativePath != null) {
            viewModel.onExportTargetPicked(relativePath, uri)
        }
    }

    AppScaffold(
        title = { Text(text = stringResource(id = R.string.inspector_title), style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = {
                if (uiState.path.isNotEmpty()) viewModel.navigateUp() else navigate(Destinations.Up)
            }) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
            }
        },
        scrollBehavior = null,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            uiState.export?.let { export ->
                TransferCard(
                    export = export,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    onCancel = { viewModel.cancelExport() },
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (val panel = uiState.panel) {
                    is InspectorPanelState.Loading -> InfoCard(stringResource(id = R.string.inspector_loading_files))
                    is InspectorPanelState.PrivilegedRequired -> PrivilegedRequiredPanel()
                    is InspectorPanelState.Error ->
                        ErrorPanel(messageRes = panel.messageRes, onRetry = { viewModel.refresh() })
                    is InspectorPanelState.Ready -> ReadyPanel(
                        uiState = uiState,
                        viewModel = viewModel,
                        onExportClick = { entry ->
                            exportLauncher.launch(entry.name)
                        },
                    )
                }
            }
        }
    }

    if (uiState.sheet == InspectorSheetState.IMAGE_PICKER) {
        ImagePickerSheet(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun InfoCard(text: String) {
    SimpleCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        cardTitle = stringResource(id = R.string.inspector_title),
    ) {
        ExpressiveIndeterminateLoadingBar(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            progressColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivilegedRequiredPanel() {
    SimpleCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        cardTitle = stringResource(id = R.string.partitions_privileged_title),
        text = stringResource(id = R.string.inspector_privileged_description),
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
    uiState: GsiInspectorUiState,
    viewModel: GsiInspectorViewModel,
    onExportClick: (com.dsu.extended.model.DsuFileEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "image_selector") {
            SimpleCard(
                modifier = Modifier.fillMaxWidth(),
                cardTitle = stringResource(id = R.string.inspector_pick_image),
                text = uiState.selectedImage?.label ?: stringResource(id = R.string.partitions_empty),
            ) {
                TextButton(onClick = { viewModel.openImagePicker() }) {
                    Text(stringResource(id = R.string.inspector_pick_image))
                }
            }
        }
        item(key = "metadata") { MetadataCard(uiState = uiState) }
        item(key = "breadcrumb") { BreadcrumbRow(segments = uiState.path, onNavigate = viewModel::navigateTo) }
        if (uiState.filesLoading) {
            item(key = "files_loading") {
                ExpressiveIndeterminateLoadingBar(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    progressColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                )
            }
        } else if (uiState.entries.isEmpty()) {
            item(key = "files_empty") {
                SimpleCard {
                    Text(
                        text = "(\u00AF\u25E1\u00AF)\u30CE \u30B8 Nothing here...",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                }
            }
        } else {
            uiState.visibleRows.forEach { row ->
                item(key = "entry_${row.relativePath}") {
                    FileRow(
                        entry = row,
                        enabled = uiState.export == null,
                        expanded = row.relativePath in uiState.expandedFolders,
                        onExportClick = onExportClick,
                        onToggleFolder = viewModel::toggleFolder,
                    )
                }
            }        }
    }
}

@Composable
private fun FileRow(
    entry: com.dsu.extended.model.DsuFileEntry,
    enabled: Boolean,
    expanded: Boolean,
    onExportClick: (com.dsu.extended.model.DsuFileEntry) -> Unit,
    onToggleFolder: (String) -> Unit,
) {
    val depth = entry.relativePath.count { it == '/' }
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (20 * depth).dp)
            .clickable(enabled = entry.isDirectory) { onToggleFolder(entry.relativePath) },
        leadingContent = {
            Icon(
                imageVector = when {
                    entry.isDirectory && expanded -> Icons.Rounded.FolderOpen
                    entry.isDirectory -> Icons.Rounded.Folder
                    else -> Icons.Rounded.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
        },
        supportingContent = if (entry.isDirectory) {
            { Text(text = stringResource(id = R.string.inspector_folder_label)) }
        } else {
            { Text(text = formatSize(entry.sizeBytes)) }
        },
        trailingContent = if (entry.isDirectory) {
            null
        } else {
            {
                IconButton(enabled = enabled, onClick = { onExportClick(entry) }) {
                    Icon(imageVector = Icons.Rounded.SaveAs, contentDescription = null)
                }
            }
        },
    )
}

@Composable
private fun BreadcrumbRow(segments: List<String>, onNavigate: (List<String>) -> Unit) {
    CardBox(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onNavigate(emptyList()) }) {
                Text(stringResource(id = R.string.inspector_breadcrumb_root))
            }
            segments.forEach { segment ->
                Text(text = "/", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    val index = segments.indexOf(segment)
                    onNavigate(segments.take(index + 1))
                }) {
                    Text(segment)
                }
            }
        }
    }
}

@Composable
private fun MetadataCard(uiState: GsiInspectorUiState) {
    CardBox(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.inspector_metadata_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (uiState.metadataLoading) {
            ExpressiveIndeterminateLoadingBar(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                progressColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            )
        } else {
            MetadataRows(metadata = uiState.metadata)
        }
    }
}

@Composable
private fun MetadataRows(metadata: DsuSystemMetadata?) {
    if (metadata == null) {
        Text(
            text = stringResource(id = R.string.inspector_mount_failed),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    val rows = listOf(
        stringResource(id = R.string.inspector_meta_android_version) to metadata.androidVersion,
        stringResource(id = R.string.inspector_meta_sdk_version) to metadata.sdkVersion.toString(),
        stringResource(id = R.string.inspector_meta_cpu_abi) to metadata.cpuAbi,
        stringResource(id = R.string.inspector_meta_vndk) to metadata.vndkVersion,
        stringResource(id = R.string.inspector_meta_security_patch) to metadata.securityPatch,
        stringResource(id = R.string.inspector_meta_fingerprint) to metadata.buildFingerprint,
        stringResource(id = R.string.inspector_meta_treble) to stringResource(
            id = if (metadata.isTrebleCompliant) R.string.yes else R.string.no,
        ),
    )
    rows.forEach { (label, value) ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun TransferCard(
    export: InspectorExportState,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
) {
    CardBox(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.partitions_progress_exporting, export.relativePath),
            style = MaterialTheme.typography.titleMedium,
        )
        if (export.isIndeterminate) {
            ExpressiveIndeterminateLoadingBar(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                progressColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            )
        } else {
            ExpressiveProgressBar(
                progress = export.copiedBytes.toFloat() / export.totalBytes.toFloat(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                progressColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            )
            Text(
                text = stringResource(
                    id = R.string.partitions_progress_bytes,
                    formatGb(export.copiedBytes),
                    formatGb(export.totalBytes),
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

@Composable
private fun ImagePickerSheet(
    uiState: GsiInspectorUiState,
    viewModel: GsiInspectorViewModel,
) {
    DialogLikeBottomSheet(
        icon = Icons.Rounded.Folder,
        title = stringResource(id = R.string.inspector_pick_image),
        confirmText = stringResource(id = R.string.partitions_confirm),
        cancelText = stringResource(id = R.string.partitions_cancel),
        onClickCancel = { viewModel.dismissImagePicker() },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                uiState.images.forEachIndexed { index, image ->
                    PreferenceItem(
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = uiState.images.size),
                        title = image.name,
                        description = image.prefix.trim('/'),
                        icon = Icons.Rounded.Folder,
                        onClick = { viewModel.selectImage(image) },
                    )
                }
            }
        },
    )
}

private fun formatGb(bytes: Long): String {
    return String.format(Locale.US, "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1L shl 30 -> String.format(Locale.US, "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
        bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
        bytes >= 1L shl 10 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
