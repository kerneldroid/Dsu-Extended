package com.dsu.extended.ui.cards.installation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dsu.extended.R
import com.dsu.extended.preparation.InstallationStep
import com.dsu.extended.ui.cards.installation.content.NotInstallingCardContent
import com.dsu.extended.ui.cards.installation.content.ProgressableCardContent
import com.dsu.extended.ui.screen.home.InstallationCardState

@Composable
fun InstallationCardStep(
    uiState: InstallationCardState,
    textFieldInteraction: MutableInteractionSource,
    minPercentageOfFreeStorage: String = "40",
    onClickClear: () -> Unit,
    onClickInstall: () -> Unit,
    onClickRetryInstallation: () -> Unit,
    onClickUnmountSdCardAndRetry: () -> Unit,
    onClickSetSeLinuxPermissive: () -> Unit,
    onClickCancelInstallation: () -> Unit,
    onClickDiscardInstalledGsiAndInstall: () -> Unit,
    onClickDiscardDsu: () -> Unit,
    onClickRebootToDynOS: () -> Unit,
    onClickOpenLogsTab: () -> Unit,
    onClickViewLogs: () -> Unit,
    onClickViewCommands: () -> Unit,
) {
    when (uiState.installationStep) {
        InstallationStep.NOT_INSTALLING ->
            NotInstallingCardContent(
                textFieldInteraction = textFieldInteraction,
                uiState = uiState,
                onClickClear = onClickClear,
                onClickInstall = onClickInstall,
            )
        InstallationStep.DSU_ALREADY_INSTALLED ->
            ProgressableCardContent(
                text = stringResource(R.string.dsu_already_installed),
                textFirstButton = stringResource(id = R.string.reboot_into_dsu),
                onClickFirstButton = onClickRebootToDynOS,
                textSecondButton = stringResource(id = R.string.discard),
                onClickSecondButton = onClickDiscardDsu,
                auxActionContentDescription = stringResource(id = R.string.logs_open_tab_content_desc),
                onClickAuxAction = onClickOpenLogsTab,
            )
        InstallationStep.DSU_ALREADY_RUNNING_DYN_OS ->
            ProgressableCardContent(
                text = stringResource(R.string.already_running_dsu),
            )
        InstallationStep.PROCESSING ->
            ProgressableCardContent(
                text = stringResource(R.string.processing),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                isIndeterminate = true,
            )
        InstallationStep.COPYING_FILE ->
            ProgressableCardContent(
                text = stringResource(R.string.copying_file),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                progress = uiState.installationProgress,
            )
        InstallationStep.DECOMPRESSING_XZ ->
            ProgressableCardContent(
                text = stringResource(R.string.decompressing_xz),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                progress = uiState.installationProgress,
            )
        InstallationStep.COMPRESSING_TO_GZ ->
            ProgressableCardContent(
                text = stringResource(R.string.compressing_to_gz),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                progress = uiState.installationProgress,
            )
        InstallationStep.DECOMPRESSING_GZIP ->
            ProgressableCardContent(
                text = stringResource(R.string.extracting_file),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                progress = uiState.installationProgress,
            )
        InstallationStep.EXTRACTING_FILE ->
            ProgressableCardContent(
                text = stringResource(R.string.extracting_file),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                progress = uiState.installationProgress,
            )
        InstallationStep.VALIDATING_IMAGE ->
            ProgressableCardContent(
                text = stringResource(id = R.string.validating_image),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                isIndeterminate = true,
            )
        InstallationStep.DISCARD_CURRENT_GSI -> {
            ProgressableCardContent(
                text = stringResource(R.string.discard_dsu_otg),
                textFirstButton = stringResource(id = R.string.discard_dsu),
                onClickFirstButton = onClickDiscardInstalledGsiAndInstall,
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                progress = uiState.installationProgress,
            )
        }
        InstallationStep.WAITING_USER_CONFIRMATION -> {
            ProgressableCardContent(
                text = stringResource(R.string.installation_prompt),
                textFirstButton = stringResource(id = R.string.try_again),
                onClickFirstButton = onClickRetryInstallation,
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
            )
        }
        InstallationStep.PROCESSING_LOG_READABLE ->
            ProgressableCardContent(
                text = stringResource(R.string.installing),
                textFirstButton = stringResource(id = R.string.cancel),
                onClickFirstButton = onClickCancelInstallation,
                textSecondButton = stringResource(id = R.string.view_logs),
                onClickSecondButton = onClickViewLogs,
                showProgressBar = true,
                isIndeterminate = true,
            )
        InstallationStep.INSTALLING -> {
            ProgressableCardContent(
                text = stringResource(R.string.installing_partition, uiState.currentPartitionText),
                textFirstButton = stringResource(id = R.string.cancel),
                onClickFirstButton = onClickCancelInstallation,
                textSecondButton = stringResource(id = R.string.view_logs),
                onClickSecondButton = onClickViewLogs,
                showProgressBar = true,
                progress = uiState.installationProgress,
            )
        }
        InstallationStep.INSTALLING_ROOTED -> {
            ProgressableCardContent(
                text = stringResource(R.string.installing_partition, uiState.currentPartitionText),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                progress = uiState.installationProgress,
            )
        }
        InstallationStep.CREATING_PARTITION ->
            ProgressableCardContent(
                text = stringResource(R.string.creating_partition, uiState.currentPartitionText),
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showProgressBar = true,
                progress = uiState.installationProgress,
            )

        // Success states
        InstallationStep.INSTALL_SUCCESS ->
            ProgressableCardContent(
                text = stringResource(R.string.installation_finished_rootless),
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showSuccess = true,
            )
        InstallationStep.INSTALL_SUCCESS_REBOOT_DYN_OS ->
            ProgressableCardContent(
                text = stringResource(R.string.installation_finished),
                textFirstButton = stringResource(id = R.string.reboot_into_dsu),
                onClickFirstButton = onClickRebootToDynOS,
                textSecondButton = stringResource(id = R.string.discard),
                onClickSecondButton = onClickDiscardDsu,
                showSuccess = true,
                auxActionContentDescription = stringResource(id = R.string.logs_open_tab_content_desc),
                onClickAuxAction = onClickOpenLogsTab,
            )
        InstallationStep.REQUIRES_ADB_CMD_TO_CONTINUE ->
            ProgressableCardContent(
                text = stringResource(R.string.require_adb_cmd_to_continue),
                textFirstButton = stringResource(id = R.string.see_commands),
                onClickFirstButton = onClickViewCommands,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
            )

        // General errors
        InstallationStep.ERROR ->
            ProgressableCardContent(
                text = stringResource(R.string.unknown_error, uiState.errorText),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
            )
        InstallationStep.ERROR_CANCELED ->
            ProgressableCardContent(
                text = stringResource(R.string.installation_canceled),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
            )
        InstallationStep.ERROR_REQUIRES_DISCARD_DSU ->
            ProgressableCardContent(
                text = stringResource(R.string.discard_dsu_otg),
                textFirstButton = stringResource(id = R.string.discard),
                onClickFirstButton = onClickDiscardInstalledGsiAndInstall,
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
            )
        InstallationStep.ERROR_ALREADY_RUNNING_DYN_OS ->
            ProgressableCardContent(
                text = stringResource(R.string.already_running_dsu),
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
            )
        InstallationStep.ERROR_CREATE_PARTITION ->
            ProgressableCardContent(
                text = stringResource(R.string.failed_create_partition),
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
            )
        InstallationStep.ERROR_EXTERNAL_SDCARD_ALLOC ->
            ProgressableCardContent(
                text = stringResource(R.string.allocation_error_description, uiState.errorText),
                textFirstButton = stringResource(id = R.string.allocation_error_action),
                onClickFirstButton = onClickUnmountSdCardAndRetry,
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showError = true,
            )
        InstallationStep.ERROR_NO_AVAIL_STORAGE ->
            ProgressableCardContent(
                text = stringResource(R.string.storage_error_description, minPercentageOfFreeStorage),
                textFirstButton = stringResource(id = R.string.try_again),
                onClickFirstButton = onClickRetryInstallation,
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showError = true,
            )
        InstallationStep.ERROR_F2FS_WRONG_PATH ->
            ProgressableCardContent(
                text = stringResource(R.string.fs_features_error_description, uiState.errorText),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.clear),
                onClickSecondButton = onClickClear,
                showError = true,
            )
        InstallationStep.ERROR_EXTENTS ->
            ProgressableCardContent(
                text = stringResource(R.string.extents_error_description),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
            )
        InstallationStep.ERROR_SELINUX ->
            ProgressableCardContent(
                text = stringResource(R.string.selinux_error_description),
                textFirstButton = stringResource(id = R.string.selinux_error_action),
                onClickFirstButton = onClickSetSeLinuxPermissive,
                textSecondButton = stringResource(id = R.string.cancel),
                onClickSecondButton = onClickCancelInstallation,
                showError = true,
            )
        InstallationStep.ERROR_SELINUX_ROOTLESS ->
            ProgressableCardContent(
                text = stringResource(R.string.selinux_error_description),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
            )

        // NEW: Enhanced boot diagnostic errors
        InstallationStep.ERROR_AVB_VERIFICATION ->
            ProgressableCardContent(
                text = "AVB Verification Failed\n\nAndroid Verified Boot is blocking the GSI. You need to disable AVB verification.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Flash disabled vbmeta:\nfastboot flash vbmeta vbmeta_disabled.img --disable-verity --disable-verification",
            )
        InstallationStep.ERROR_PARTITION_TABLE ->
            ProgressableCardContent(
                text = "Partition Table Error\n\nFailed to read or modify partition table. Device may not properly support Dynamic Partitions.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Use a patched gsid build for your Android version",
            )
        InstallationStep.ERROR_GSI_INCOMPATIBLE ->
            ProgressableCardContent(
                text =
                if (uiState.errorText.isNotEmpty()) {
                    "GSI Incompatible\n\n${uiState.errorText}"
                } else {
                    "GSI Incompatible\n\nThe selected GSI is not compatible with your device. Check architecture and VNDK version."
                },
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Use a GSI matching your device's: CPU architecture, Android version, VNDK version",
            )
        InstallationStep.ERROR_KERNEL_MISMATCH ->
            ProgressableCardContent(
                text = "Kernel Panic / Mismatch\n\nThe kernel detected a critical error. GSI may be incompatible with your device's kernel.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Try a different GSI built for your kernel version",
            )
        InstallationStep.ERROR_BOOTLOOP_DETECTED ->
            ProgressableCardContent(
                text = "Bootloop Detected\n\nThe system is rebooting repeatedly. GSI failed to boot properly.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "1. Try a different GSI\n2. Check if bootloader is unlocked\n3. Verify VNDK compatibility",
            )
        InstallationStep.ERROR_INSUFFICIENT_RAM ->
            ProgressableCardContent(
                text = "Insufficient RAM\n\nYour device may not have enough RAM to run the selected GSI.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Try a lighter GSI (Go edition or minimal builds)",
            )
        InstallationStep.ERROR_VENDOR_MISMATCH ->
            ProgressableCardContent(
                text = "Vendor Mismatch\n\nThe GSI is incompatible with your device's vendor image.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Use a GSI built for your Android version and VNDK level",
            )
        InstallationStep.ERROR_DM_VERITY ->
            ProgressableCardContent(
                text = "dm-verity Error\n\nVerified boot subsystem is blocking the GSI.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Flash disabled vbmeta with --disable-verity flag",
            )
        InstallationStep.ERROR_SYSTEM_SERVER_CRASH ->
            ProgressableCardContent(
                text = "System Server Crash\n\nAndroid system server crashed during boot. GSI may have compatibility issues.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Try a more stable GSI build from trusted sources",
            )
        InstallationStep.ERROR_BOOT_TIMEOUT ->
            ProgressableCardContent(
                text = "Boot Timeout\n\nThe system took too long to boot. GSI may be stuck or incompatible.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Wait longer on first boot, or try a different GSI",
            )
        InstallationStep.ERROR_SUPER_PARTITION ->
            ProgressableCardContent(
                text = "Super Partition Error\n\nFailed to operate on the super partition.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Ensure device properly supports Dynamic Partitions",
            )
        InstallationStep.ERROR_SLOT_SWITCH ->
            ProgressableCardContent(
                text = "Slot Switch Error\n\nFailed to switch boot slot for DSU.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
            )
        InstallationStep.ERROR_FIRMWARE_MISMATCH ->
            ProgressableCardContent(
                text = "Firmware Mismatch\n\nDevice firmware is incompatible with the GSI.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Update your device firmware to the latest version",
            )
        InstallationStep.ERROR_TREBLE_INCOMPATIBLE ->
            ProgressableCardContent(
                text = "Treble Incompatible\n\nYour device may not be Treble-compliant or has incorrect Treble implementation.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Check Treble compliance with Treble Info app",
            )
        InstallationStep.ERROR_VNDK_MISMATCH ->
            ProgressableCardContent(
                text = "VNDK Version Mismatch\n\nThe GSI's VNDK version doesn't match your device.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Check your VNDK: getprop ro.vndk.version\nUse GSI with matching VNDK",
            )
        InstallationStep.ERROR_CPU_ARCH_MISMATCH ->
            ProgressableCardContent(
                text = "CPU Architecture Mismatch\n\nThe GSI is built for a different CPU architecture.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Use GSI matching your device architecture (arm64-v8a, armeabi-v7a, etc.)",
            )
        InstallationStep.ERROR_UNKNOWN_BOOT_FAILURE ->
            ProgressableCardContent(
                text = "Unknown Boot Failure\n\nAn unidentified error occurred during boot. Check logs for details.",
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = "Review logs and search for your device + GSI on XDA forums",
            )
    }
}
