package com.dsu.extended.ui.screen.home

import com.dsu.extended.model.DiagnosticReport
import com.dsu.extended.preparation.InstallationStep

data class InstallationCardState(
    val installationStep: InstallationStep = InstallationStep.NOT_INSTALLING,
    val isTextFieldEnabled: Boolean = true,
    val isInstallable: Boolean = false,
    val isError: Boolean = false,
    val text: String = "",
    val errorText: String = "",
    val isProgressBarIndeterminate: Boolean = false,
    val installationProgress: Float = 0F,
    val currentPartitionText: String = "",
    // NEW: Diagnostic information
    val diagnosticReport: DiagnosticReport? = null,
    val estimatedTimeRemaining: String = "",
)

data class UserDataCardState(
    val isSelected: Boolean = false,
    val isError: Boolean = false,
    val text: String = "",
    val maximumAllowed: Int = 0,
)

data class ImageSizeCardState(
    val isSelected: Boolean = false,
    val text: String = "",
)

enum class AdditionalCardState {
    NONE,
    SETUP_STORAGE,
    UNAVAILABLE_STORAGE,
    NO_DYNAMIC_PARTITIONS,
    MISSING_READ_LOGS_PERMISSION,
    GRANTING_READ_LOGS_PERMISSION,
    BOOTLOADER_UNLOCKED_WARNING,

    // NEW: Device compatibility check
    DEVICE_COMPATIBILITY_CHECK,
}

enum class SheetDisplayState {
    NONE,
    IMAGESIZE_WARNING,
    CONFIRM_INSTALLATION,
    CANCEL_INSTALLATION,
    DISCARD_DSU,
    VIEW_LOGS,

    // NEW: Additional sheets
    DEVICE_INFO,
    DIAGNOSTIC_REPORT,
    INSTALLATION_HISTORY,
}

data class HomeUiState(
    val installationCard: InstallationCardState = InstallationCardState(),
    val userDataCard: UserDataCardState = UserDataCardState(),
    val imageSizeCard: ImageSizeCardState = ImageSizeCardState(),
    val additionalCard: AdditionalCardState = AdditionalCardState.NONE,
    val sheetDisplay: SheetDisplayState = SheetDisplayState.NONE,
    val installationLogs: String = "",
    val passedInitialChecks: Boolean = false,
    val shouldKeepScreenOn: Boolean = false,
    // NEW: Enhanced state fields
    val diagnosticReport: DiagnosticReport? = null,
    val showFab: Boolean = true,
    val isDeviceCompatible: Boolean = true,
) {
    fun isInstalling(): Boolean {
        return installationCard.installationStep != InstallationStep.NOT_INSTALLING
    }

    fun isError(): Boolean {
        return installationCard.installationStep.name.startsWith("ERROR")
    }

    fun isSuccess(): Boolean {
        return installationCard.installationStep.name.startsWith("INSTALL_SUCCESS")
    }
}
