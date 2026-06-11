package com.dsu.extended.ui.screen.logs

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dsu.extended.R
import com.dsu.extended.util.LogsStore
import com.dsu.extended.util.StoredLogEntry
import com.dsu.extended.util.StoredLogType

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val application: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        refreshLogs()
    }

    fun refreshLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val logs = LogsStore.listLogs(application)
            _uiState.update { state ->
                state.copy(allLogs = logs).applyFilters()
            }
        }
    }

    fun setTypeFilter(filter: LogsTypeFilter) {
        _uiState.update { it.copy(typeFilter = filter).applyFilters() }
    }

    fun setDateFilter(filter: LogsDateFilter) {
        _uiState.update { it.copy(dateFilter = filter).applyFilters() }
    }

    fun openLog(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = LogsStore.readLog(filePath)
            val title = uiState.value.allLogs.firstOrNull { it.filePath == filePath }?.title ?: ""
            _uiState.update {
                it.copy(
                    selectedLogPath = filePath,
                    selectedLogTitle = title,
                    selectedLogContent = content,
                    showLogViewer = true,
                )
            }
        }
    }

    fun closeLogViewer() {
        _uiState.update {
            it.copy(
                showLogViewer = false,
                selectedLogPath = "",
                selectedLogTitle = "",
                selectedLogContent = "",
            )
        }
    }

    fun startRename(filePath: String) {
        val title = uiState.value.allLogs.firstOrNull { it.filePath == filePath }?.title ?: ""
        _uiState.update {
            it.copy(
                renameTargetPath = filePath,
                renameText = title,
            )
        }
    }

    fun updateRenameText(text: String) {
        _uiState.update { it.copy(renameText = text) }
    }

    fun cancelRename() {
        _uiState.update { it.copy(renameTargetPath = null, renameText = "") }
    }

    fun applyRename() {
        val targetPath = uiState.value.renameTargetPath ?: return
        val renameText = uiState.value.renameText
        viewModelScope.launch(Dispatchers.IO) {
            val renamed = LogsStore.renameLog(targetPath, renameText)
            val message =
                if (renamed != null) {
                    application.getString(R.string.logs_status_renamed)
                } else {
                    application.getString(R.string.logs_status_rename_failed)
                }
            val logs = LogsStore.listLogs(application)
            _uiState.update {
                it.copy(
                    allLogs = logs,
                    renameTargetPath = null,
                    renameText = "",
                    statusMessage = message,
                ).applyFilters()
            }
        }
    }

    fun deleteLog(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = LogsStore.deleteLog(filePath)
            val message =
                if (deleted) {
                    application.getString(R.string.logs_status_deleted)
                } else {
                    application.getString(R.string.logs_status_delete_failed)
                }
            val logs = LogsStore.listLogs(application)
            _uiState.update {
                it.copy(
                    allLogs = logs,
                    statusMessage = message,
                ).applyFilters()
            }
        }
    }

    fun captureCatExperimental() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true) }
            val result = LogsStore.captureCatExperimental(application)
            val message =
                if (result.isSuccess) {
                    application.getString(R.string.logs_status_cat_saved)
                } else {
                    application.getString(
                        R.string.logs_status_cat_failed,
                        result.exceptionOrNull()?.message ?: "unknown",
                    )
                }
            val logs = LogsStore.listLogs(application)
            _uiState.update {
                it.copy(
                    allLogs = logs,
                    isBusy = false,
                    statusMessage = message,
                ).applyFilters()
            }
        }
    }

    fun consumeStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun LogsUiState.applyFilters(): LogsUiState {
        val typeFiltered =
            when (typeFilter) {
                LogsTypeFilter.ALL -> allLogs
                LogsTypeFilter.INSTALLATION -> allLogs.filter { it.type == StoredLogType.INSTALLATION }
                LogsTypeFilter.AUTO_MODE -> allLogs.filter { it.type == StoredLogType.AUTO_MODE }
                LogsTypeFilter.CAT -> allLogs.filter { it.type == StoredLogType.CAT || it.type == StoredLogType.BOOT }
            }
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfWeek = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
        val dateFiltered =
            when (dateFilter) {
                LogsDateFilter.ALL -> typeFiltered
                LogsDateFilter.TODAY -> typeFiltered.filter { it.createdAtMillis >= startOfToday }
                LogsDateFilter.WEEK -> typeFiltered.filter { it.createdAtMillis >= startOfWeek }
            }
        return copy(filteredLogs = dateFiltered)
    }
}
