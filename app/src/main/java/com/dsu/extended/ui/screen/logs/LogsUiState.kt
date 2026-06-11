package com.dsu.extended.ui.screen.logs

import com.dsu.extended.util.StoredLogEntry

enum class LogsTypeFilter {
    ALL,
    INSTALLATION,
    AUTO_MODE,
    CAT,
}

enum class LogsDateFilter {
    ALL,
    TODAY,
    WEEK,
}

data class LogsUiState(
    val allLogs: List<StoredLogEntry> = emptyList(),
    val filteredLogs: List<StoredLogEntry> = emptyList(),
    val typeFilter: LogsTypeFilter = LogsTypeFilter.ALL,
    val dateFilter: LogsDateFilter = LogsDateFilter.ALL,
    val isBusy: Boolean = false,
    val selectedLogPath: String = "",
    val selectedLogTitle: String = "",
    val selectedLogContent: String = "",
    val showLogViewer: Boolean = false,
    val renameTargetPath: String? = null,
    val renameText: String = "",
    val statusMessage: String? = null,
)
