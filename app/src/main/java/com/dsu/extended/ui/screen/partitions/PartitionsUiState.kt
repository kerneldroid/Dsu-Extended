package com.dsu.extended.ui.screen.partitions

import android.net.Uri

data class DsuImageEntry(
    val prefix: String,
    val name: String,
)

data class PartitionGroup(
    val prefix: String,
    val images: List<DsuImageEntry>,
)

data class PartitionTransferState(
    val imageName: String,
    val kind: PartitionTransferKind,
    val copiedBytes: Long = 0L,
    val totalBytes: Long = -1L,
) {
    val isIndeterminate: Boolean get() = totalBytes <= 0L
}

enum class PartitionTransferKind {
    ADD,
    REPLACE,
    EXPORT,
}

sealed interface PartitionsPanelState {
    data object Loading : PartitionsPanelState
    data object PrivilegedRequired : PartitionsPanelState
    data object Ready : PartitionsPanelState
    data class Error(val messageRes: Int) : PartitionsPanelState
}

enum class PartitionsSheetState {
    NONE,
    ADD_IMAGE,
    CONFIRM_REPLACE,
    CONFIRM_DELETE,
}

data class PendingAddImage(
    val prefix: String,
    val name: String,
    val sourceUri: Uri,
)

data class PendingReplaceImage(
    val entry: DsuImageEntry,
    val sourceUri: Uri,
)

data class PartitionsUiState(
    val panel: PartitionsPanelState = PartitionsPanelState.Loading,
    val groups: List<PartitionGroup> = emptyList(),
    val transfer: PartitionTransferState? = null,
    val sheet: PartitionsSheetState = PartitionsSheetState.NONE,
    val pendingAdd: PendingAddImage? = null,
    val pendingReplace: PendingReplaceImage? = null,
    val pendingDelete: DsuImageEntry? = null,
)
