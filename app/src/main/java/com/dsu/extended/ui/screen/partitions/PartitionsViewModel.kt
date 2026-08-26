package com.dsu.extended.ui.screen.partitions

import android.app.Application
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dsu.extended.IPartitionTransferListener
import com.dsu.extended.R
import com.dsu.extended.core.BaseViewModel
import com.dsu.extended.model.Session
import com.dsu.extended.service.PrivilegedProvider
import com.dsu.extended.util.OperationMode
import com.dsu.extended.util.PartitionResult

@HiltViewModel
class PartitionsViewModel @Inject constructor(
    val application: Application,
    override val dataStore: DataStore<Preferences>,
    private val session: Session,
) : BaseViewModel(dataStore) {

    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(PartitionsUiState())
    val uiState: StateFlow<PartitionsUiState> = _uiState.asStateFlow()

    private var exportDestination: ParcelFileDescriptor? = null
    private var refreshJob: Job? = null
    private var replaceTarget: DsuImageEntry? = null

    fun markReplaceTarget(entry: DsuImageEntry) {
        replaceTarget = entry
    }

    private val transferListener = object : IPartitionTransferListener.Stub() {
        override fun onProgress(copiedBytes: Long, totalBytes: Long) {
            _uiState.update {
                it.copy(
                    transfer = it.transfer?.copy(
                        copiedBytes = copiedBytes,
                        totalBytes = totalBytes,
                    ),
                )
            }
        }

        override fun onCompleted(resultCode: Int) {
            exportDestination?.let { runCatching { it.close() } }
            exportDestination = null
            if (resultCode == PartitionResult.OK) {
                refresh()
            }
            _uiState.update { state ->
                state.copy(
                    transfer = null,
                    panel = if (resultCode == PartitionResult.OK) {
                        PartitionsPanelState.Ready
                    } else {
                        PartitionsPanelState.Error(messageFor(resultCode))
                    },
                )
            }
        }
    }

    init {
        refresh()
    }

    fun refresh() {
        if (!isPrivileged()) {
            _uiState.update { it.copy(panel = PartitionsPanelState.PrivilegedRequired, groups = emptyList()) }
            return
        }
        _uiState.update { it.copy(panel = PartitionsPanelState.Loading) }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            PrivilegedProvider.run(
                onFail = {
                    _uiState.update { it.copy(panel = PartitionsPanelState.Loading) }
                },
            ) {
                val prefixes = getImagePrefixes()
                val groups = prefixes.mapNotNull { prefix ->
                    val names = getDsuImages(prefix)
                    if (names.isEmpty()) {
                        null
                    } else {
                        PartitionGroup(prefix, names.sorted().map { DsuImageEntry(prefix, it) })
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        panel = if (state.transfer == null) PartitionsPanelState.Ready else state.panel,
                        groups = groups,
                    )
                }
            }
        }
    }

    fun onReplacementFilePicked(uri: Uri) {
        val entry = replaceTarget ?: return
        _uiState.update {
            it.copy(
                sheet = PartitionsSheetState.CONFIRM_REPLACE,
                pendingReplace = PendingReplaceImage(entry, uri),
            )
        }
    }

    fun confirmReplaceImage() {
        val pending = _uiState.value.pendingReplace ?: return
        _uiState.update { it.copy(sheet = PartitionsSheetState.NONE) }
        startTransfer(kind = PartitionTransferKind.REPLACE, imageName = pending.entry.name) {
            val fd = openSource(pending.sourceUri)
            val size = fileSize(pending.sourceUri)
            if (fd == null || size <= 0L) {
                runCatching { fd?.close() }
                transferListener.onCompleted(PartitionResult.SIZE_INVALID)
                return@startTransfer
            }
            val accepted = startReplaceDsuImage(
                pending.entry.prefix,
                pending.entry.name,
                fd,
                size,
                false,
                transferListener,
            )
            if (!accepted) {
                fd.close()
                transferListener.onCompleted(PartitionResult.BUSY)
            }
        }
        _uiState.update { it.copy(pendingReplace = null) }
    }

    fun onImagePickedForAdd(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val name = queryDisplayName(uri)?.removeSuffix(".img") ?: return@launch
            val prefix = defaultPrefix() ?: return@launch
            _uiState.update {
                it.copy(
                    sheet = PartitionsSheetState.ADD_IMAGE,
                    pendingAdd = PendingAddImage(prefix, name, uri),
                )
            }
        }
    }

    fun onPendingAddNameChange(name: String) {
        _uiState.update { it.copy(pendingAdd = it.pendingAdd?.copy(name = name)) }
    }

    fun confirmAddImage(readOnly: Boolean) {
        val pending = _uiState.value.pendingAdd ?: return
        val sanitized = pending.name.trim()
        if (!NAME_REGEX.matches(sanitized)) {
            _uiState.update { it.copy(sheet = PartitionsSheetState.NONE, panel = PartitionsPanelState.Error(R.string.partitions_invalid_name)) }
            return
        }
        val duplicate = _uiState.value.groups.any { group ->
            group.prefix == pending.prefix && group.images.any { it.name == sanitized }
        }
        if (duplicate) {
            _uiState.update { it.copy(sheet = PartitionsSheetState.NONE, panel = PartitionsPanelState.Error(R.string.partitions_duplicate)) }
            return
        }
        _uiState.update { it.copy(sheet = PartitionsSheetState.NONE) }
        startTransfer(
            kind = PartitionTransferKind.ADD,
            imageName = sanitized,
        ) {
            val fd = openSource(pending.sourceUri)
            val size = fileSize(pending.sourceUri)
            if (fd == null || size <= 0L) {
                runCatching { fd?.close() }
                transferListener.onCompleted(PartitionResult.SIZE_INVALID)
                return@startTransfer
            }
            val accepted = startAddDsuImage(pending.prefix, sanitized, fd, size, readOnly, transferListener)
            if (!accepted) {
                fd.close()
                transferListener.onCompleted(PartitionResult.BUSY)
            }
        }
    }

    fun onExportTargetPicked(entry: DsuImageEntry, uri: Uri) {
        val fd = application.contentResolver.openFileDescriptor(uri, "wt")
        if (fd == null) {
            _uiState.update { it.copy(panel = PartitionsPanelState.Error(R.string.partitions_export_failed)) }
            return
        }
        exportDestination = fd
        startExport(entry, fd)
    }

    fun showDeleteConfirmation(entry: DsuImageEntry) {
        _uiState.update {
            it.copy(sheet = PartitionsSheetState.CONFIRM_DELETE, pendingDelete = entry)
        }
    }

    fun confirmDeleteImage() {
        val entry = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(sheet = PartitionsSheetState.NONE) }
        viewModelScope.launch(Dispatchers.IO) {
            PrivilegedProvider.run(
                onFail = {
                    _uiState.update { it.copy(panel = PartitionsPanelState.Error(R.string.partitions_service_unavailable)) }
                },
            ) {
                val code = deleteDsuImage(entry.prefix, entry.name)
                if (code == PartitionResult.OK) {
                    refresh()
                } else {
                    _uiState.update { it.copy(panel = PartitionsPanelState.Error(messageFor(code))) }
                }
            }
        }
    }

    fun cancelTransfer() {
        if (!PrivilegedProvider.isConnected()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { PrivilegedProvider.getService().cancelDsuImageTransfer() }
        }
    }

    fun dismissError() {
        _uiState.update {
            if (it.panel is PartitionsPanelState.Error) it.copy(panel = PartitionsPanelState.Ready) else it
        }
    }

    fun dismissSheet() {
        _uiState.update { it.copy(sheet = PartitionsSheetState.NONE, pendingDelete = null, pendingAdd = null) }
    }

    private fun startExport(entry: DsuImageEntry, fd: ParcelFileDescriptor) {
        startTransfer(kind = PartitionTransferKind.EXPORT, imageName = entry.name) {
            val accepted = startExportDsuImage(entry.prefix, entry.name, fd, transferListener)
            if (!accepted) {
                fd.close()
                exportDestination = null
                transferListener.onCompleted(PartitionResult.BUSY)
            }
        }
    }

    private inline fun startTransfer(
        kind: PartitionTransferKind,
        imageName: String,
        crossinline operation: com.dsu.extended.IPrivilegedService.() -> Unit,
    ) {
        _uiState.update {
            it.copy(
                transfer = PartitionTransferState(imageName = imageName, kind = kind),
                panel = PartitionsPanelState.Ready,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            PrivilegedProvider.run(
                onFail = { transferListener.onCompleted(PartitionResult.SERVICE_UNAVAILABLE) },
            ) {
                this.operation()
            }
        }
    }

    private fun openSource(uri: Uri): ParcelFileDescriptor? {
        return application.contentResolver.openFileDescriptor(uri, "r")
    }

    private fun fileSize(uri: Uri): Long {
        return runCatching {
            application.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }.getOrDefault(-1L)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            application.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()?.takeIf { NAME_REGEX.matches(it.removeSuffix(".img")) }
    }

    private fun defaultPrefix(): String? {
        return _uiState.value.groups.firstOrNull()?.prefix
    }

    private fun isPrivileged(): Boolean {
        return when (session.getOperationMode()) {
            OperationMode.ROOT, OperationMode.SYSTEM, OperationMode.SYSTEM_AND_ROOT -> true
            else -> false
        }
    }

    private fun messageFor(code: Int): Int {
        return when (code) {
            PartitionResult.OK -> R.string.partitions_done
            PartitionResult.BUSY -> R.string.partitions_busy
            PartitionResult.INVALID_NAME -> R.string.partitions_invalid_name
            PartitionResult.DUPLICATE -> R.string.partitions_duplicate
            PartitionResult.NOT_FOUND -> R.string.partitions_not_found
            PartitionResult.SIZE_INVALID -> R.string.partitions_size_invalid
            PartitionResult.NO_SPACE -> R.string.partitions_no_space
            PartitionResult.CANCELLED -> R.string.partitions_cancelled
            else -> R.string.partitions_operation_failed
        }
    }

    private companion object {
        val NAME_REGEX = Regex("[A-Za-z0-9_.-]+")
    }
}
