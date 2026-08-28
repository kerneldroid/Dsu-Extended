package com.dsu.extended.ui.screen.inspector

import com.dsu.extended.model.DsuFileEntry
import com.dsu.extended.model.DsuSystemMetadata

data class InspectorImageOption(
    val prefix: String,
    val name: String,
) {
    val label: String get() = "${prefix.trim('/')}/$name"
}

sealed interface InspectorPanelState {
    data object Loading : InspectorPanelState
    data object PrivilegedRequired : InspectorPanelState
    data object Ready : InspectorPanelState
    data class Error(val messageRes: Int) : InspectorPanelState
}

data class InspectorExportState(
    val relativePath: String,
    val copiedBytes: Long = 0L,
    val totalBytes: Long = -1L,
) {
    val isIndeterminate: Boolean get() = totalBytes <= 0L
}

enum class InspectorSheetState {
    NONE,
    IMAGE_PICKER,
}

data class GsiInspectorUiState(
    val panel: InspectorPanelState = InspectorPanelState.Loading,
    val images: List<InspectorImageOption> = emptyList(),
    val selectedImage: InspectorImageOption? = null,
    val metadata: DsuSystemMetadata? = null,
    val metadataLoading: Boolean = false,
    val path: List<String> = emptyList(),
    val entries: List<DsuFileEntry> = emptyList(),
    val filesLoading: Boolean = false,
    val expandedFolders: Set<String> = emptySet(),
    val childEntries: Map<String, List<DsuFileEntry>> = emptyMap(),
    val childLoadingPaths: Set<String> = emptySet(),
    val visibleRows: List<DsuFileEntry> = emptyList(),
    val export: InspectorExportState? = null,
    val sheet: InspectorSheetState = InspectorSheetState.NONE,
)
