package com.dsu.extended.ui.screen.inspector

import android.app.Application
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
import com.dsu.extended.model.DsuFileEntry
import com.dsu.extended.model.DsuSystemMetadata
import com.dsu.extended.service.PrivilegedProvider
import com.dsu.extended.util.OperationMode
import com.dsu.extended.util.PartitionResult

@HiltViewModel
class GsiInspectorViewModel @Inject constructor(
    val application: Application,
    override val dataStore: DataStore<Preferences>,
    private val session: com.dsu.extended.model.Session,
) : BaseViewModel(dataStore) {

    private val _uiState = MutableStateFlow(GsiInspectorUiState())
    val uiState: StateFlow<GsiInspectorUiState> = _uiState.asStateFlow()

    private var exportDestination: ParcelFileDescriptor? = null
    private var pendingExportPath: String? = null

    private val transferListener = object : IPartitionTransferListener.Stub() {
        override fun onProgress(copiedBytes: Long, totalBytes: Long) {
            _uiState.update { state ->
                state.copy(
                    export = state.export?.copy(
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
                    export = null,
                    panel = if (resultCode == PartitionResult.OK) {
                        state.panel
                    } else {
                        InspectorPanelState.Error(R.string.inspector_export_failed)
                    },
                )
            }
        }
    }

    init {
        refresh()
    }

    fun refresh() {
        if (!isRootMode()) {
            _uiState.update { it.copy(panel = InspectorPanelState.PrivilegedRequired, images = emptyList()) }
            return
        }
        _uiState.update { it.copy(panel = InspectorPanelState.Loading) }
        viewModelScope.launch(Dispatchers.IO) {
            PrivilegedProvider.run(
                onFail = {
                    _uiState.update { it.copy(panel = InspectorPanelState.Error(R.string.partitions_service_unavailable)) }
                },
            ) {
                val options = getImagePrefixes()
                    .flatMap { prefix -> getDsuImages(prefix).map { InspectorImageOption(prefix, it) } }
                _uiState.update { state ->
                    state.copy(
                        images = options,
                        panel = InspectorPanelState.Ready,
                        selectedImage = state.selectedImage?.takeIf { selected ->
                            options.any { it.prefix == selected.prefix && it.name == selected.name }
                        },
                    )
                }
                selectImage(_uiState.value.selectedImage ?: options.firstOrNull())
            }
        }
    }

    fun selectImage(image: InspectorImageOption?) {
        if (image == null || image == _uiState.value.selectedImage) return
        _uiState.update {
            it.copy(
                selectedImage = image,
                metadata = null,
                metadataLoading = true,
                path = emptyList(),
                entries = emptyList(),
                filesLoading = true,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            PrivilegedProvider.run(
                onFail = {
                    _uiState.update {
                        it.copy(metadataLoading = false, filesLoading = false, panel = InspectorPanelState.Error(R.string.partitions_service_unavailable))
                    }
                },
            ) {
                val metadata = inspectGsiMetadata(image.prefix, image.name)
                val entries = listPartitionFiles(image.prefix, image.name, "")
                if (entries == null) {
                    _uiState.update {
                        it.copy(
                            metadata = metadata,
                            metadataLoading = false,
                            filesLoading = false,
                            panel = InspectorPanelState.Error(R.string.partitions_service_unavailable),
                        )
                    }
                    return@run
                }
                _uiState.update {
                    it.copy(
                        metadata = metadata,
                        metadataLoading = false,
                        entries = entries,
                        filesLoading = false,
                    )
                }
            }
        }
    }

    fun openDirectory(entry: DsuFileEntry) {
        if (!entry.isDirectory) return
        toggleFolder(entry.relativePath)
    }

    fun toggleFolder(relativePath: String) {
        val state = _uiState.value
        val isExpanded = relativePath in state.expandedFolders
        if (isExpanded) {
            val next = state.expandedFolders - relativePath
            _uiState.update { it.copy(expandedFolders = next) }
            rebuildVisibleRows(next, state.childEntries)
            return
        }
        _uiState.update { it.copy(expandedFolders = state.expandedFolders + relativePath) }
        if (state.childEntries.containsKey(relativePath)) {
            rebuildVisibleRows(state.expandedFolders + relativePath, state.childEntries)
            return
        }
        val image = state.selectedImage ?: return
        _uiState.update { it.copy(childLoadingPaths = state.childLoadingPaths + relativePath) }
        viewModelScope.launch(Dispatchers.IO) {
            PrivilegedProvider.run(
                onFail = {
                    _uiState.update {
                        it.copy(
                            childLoadingPaths = it.childLoadingPaths - relativePath,
                            panel = InspectorPanelState.Error(R.string.partitions_service_unavailable),
                        )
                    }
                },
            ) {
                val children = listPartitionFiles(image.prefix, image.name, relativePath)
                if (children == null) {
                    _uiState.update { current ->
                        current.copy(
                            childLoadingPaths = current.childLoadingPaths - relativePath,
                            expandedFolders = current.expandedFolders - relativePath,
                            panel = InspectorPanelState.Error(R.string.partitions_service_unavailable),
                        )
                    }
                    return@run
                }
                _uiState.update { current ->
                    current.copy(
                        childEntries = current.childEntries + (relativePath to children),
                        childLoadingPaths = current.childLoadingPaths - relativePath,
                        expandedFolders = current.expandedFolders + relativePath,
                    )
                }
                rebuildVisibleRows(
                    _uiState.value.expandedFolders,
                    _uiState.value.childEntries,
                )
            }
        }
    }

    private fun rebuildVisibleRows(expanded: Set<String>, childEntries: Map<String, List<DsuFileEntry>>) {
        val rows = mutableListOf<DsuFileEntry>()
        fun walk(entries: List<DsuFileEntry>) {
            entries.forEach { entry ->
                rows += entry
                if (entry.isDirectory && entry.relativePath in expanded) {
                    childEntries[entry.relativePath]?.let { walk(it) }
                }
            }
        }
        walk(_uiState.value.entries)
        _uiState.update { it.copy(visibleRows = rows.toList()) }
    }

    fun navigateUp() {
        val path = _uiState.value.path
        if (path.isNotEmpty()) navigateTo(path.dropLast(1))
    }

    fun navigateTo(segments: List<String>) {
        val image = _uiState.value.selectedImage ?: return
        _uiState.update { it.copy(path = segments, filesLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            PrivilegedProvider.run(
                onFail = {
                    _uiState.update { it.copy(filesLoading = false) }
                },
            ) {
                val entries = listPartitionFiles(image.prefix, image.name, segments.joinToString("/")) ?: emptyList()
                _uiState.update { it.copy(entries = entries, filesLoading = false) }
            }
        }
    }

    fun onExportTargetPicked(relativePath: String, uri: android.net.Uri) {
        val image = _uiState.value.selectedImage ?: return
        val fd = application.contentResolver.openFileDescriptor(uri, "wt")
        if (fd == null) {
            _uiState.update { it.copy(panel = InspectorPanelState.Error(R.string.inspector_export_failed)) }
            return
        }
        exportDestination = fd
        _uiState.update {
            it.copy(export = InspectorExportState(relativePath = relativePath))
        }
        viewModelScope.launch(Dispatchers.IO) {
            PrivilegedProvider.run(
                onFail = {
                    runCatching { fd.close() }
                    exportDestination = null
                    _uiState.update { it.copy(export = null, panel = InspectorPanelState.Error(R.string.partitions_service_unavailable)) }
                },
            ) {
                val accepted = startExportPartitionFile(
                    image.prefix,
                    image.name,
                    relativePath,
                    fd,
                    transferListener,
                )
                if (!accepted) {
                    runCatching { fd.close() }
                    exportDestination = null
                    _uiState.update { it.copy(export = null) }
                }
            }
        }
    }

    fun cancelExport() {
        if (!PrivilegedProvider.isConnected()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { PrivilegedProvider.getService().cancelDsuImageTransfer() }
        }
    }

    fun openImagePicker() {
        _uiState.update { it.copy(sheet = InspectorSheetState.IMAGE_PICKER) }
    }

    fun dismissImagePicker() {
        _uiState.update { it.copy(sheet = InspectorSheetState.NONE) }
    }

    fun dismissError() {
        _uiState.update {
            if (it.panel is InspectorPanelState.Error) it.copy(panel = InspectorPanelState.Ready) else it
        }
    }

    private fun isRootMode(): Boolean {
        return when (session.getOperationMode()) {
            OperationMode.ROOT, OperationMode.SYSTEM, OperationMode.SYSTEM_AND_ROOT -> true
            else -> false
        }
    }
}
