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
                progress = uiState.installationProgress,
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
                text = stringResource(id = R.string.boot_err_avb_title) + "\n\n" + stringResource(id = R.string.boot_err_avb_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_avb_suggestion),
            )
        InstallationStep.ERROR_PARTITION_TABLE ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_partition_table_title) + "\n\n" + stringResource(id = R.string.boot_err_partition_table_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_partition_table_suggestion),
            )
        InstallationStep.ERROR_GSI_INCOMPATIBLE ->
            ProgressableCardContent(
                text =
                if (uiState.errorText.isNotEmpty()) {
                    stringResource(id = R.string.boot_err_gsi_incompatible_title) + "\n\n" + uiState.errorText
                } else {
                    stringResource(id = R.string.boot_err_gsi_incompatible_title) + "\n\n" + stringResource(id = R.string.boot_err_gsi_incompatible_detail)
                },
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_gsi_incompatible_suggestion),
            )
        InstallationStep.ERROR_KERNEL_MISMATCH ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_kernel_title) + "\n\n" + stringResource(id = R.string.boot_err_kernel_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_kernel_suggestion),
            )
        InstallationStep.ERROR_BOOTLOOP_DETECTED ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_bootloop_title) + "\n\n" + stringResource(id = R.string.boot_err_bootloop_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_bootloop_suggestion),
            )
        InstallationStep.ERROR_INSUFFICIENT_RAM ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_ram_title) + "\n\n" + stringResource(id = R.string.boot_err_ram_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_ram_suggestion),
            )
        InstallationStep.ERROR_VENDOR_MISMATCH ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_vendor_title) + "\n\n" + stringResource(id = R.string.boot_err_vendor_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_vendor_suggestion),
            )
        InstallationStep.ERROR_DM_VERITY ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_dm_verity_title) + "\n\n" + stringResource(id = R.string.boot_err_dm_verity_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_dm_verity_suggestion),
            )
        InstallationStep.ERROR_SYSTEM_SERVER_CRASH ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_system_server_title) + "\n\n" + stringResource(id = R.string.boot_err_system_server_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_system_server_suggestion),
            )
        InstallationStep.ERROR_BOOT_TIMEOUT ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_boot_timeout_title) + "\n\n" + stringResource(id = R.string.boot_err_boot_timeout_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_boot_timeout_suggestion),
            )
        InstallationStep.ERROR_SUPER_PARTITION ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_super_title) + "\n\n" + stringResource(id = R.string.boot_err_super_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_super_suggestion),
            )
        InstallationStep.ERROR_SLOT_SWITCH ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_slot_switch_title) + "\n\n" + stringResource(id = R.string.boot_err_slot_switch_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
            )
        InstallationStep.ERROR_FIRMWARE_MISMATCH ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_firmware_title) + "\n\n" + stringResource(id = R.string.boot_err_firmware_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_firmware_suggestion),
            )
        InstallationStep.ERROR_TREBLE_INCOMPATIBLE ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_treble_title) + "\n\n" + stringResource(id = R.string.boot_err_treble_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_treble_suggestion),
            )
        InstallationStep.ERROR_VNDK_MISMATCH ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_vndk_title) + "\n\n" + stringResource(id = R.string.boot_err_vndk_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_vndk_suggestion),
            )
        InstallationStep.ERROR_CPU_ARCH_MISMATCH ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_cpu_arch_title) + "\n\n" + stringResource(id = R.string.boot_err_cpu_arch_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_cpu_arch_suggestion),
            )
        InstallationStep.ERROR_UNKNOWN_BOOT_FAILURE ->
            ProgressableCardContent(
                text = stringResource(id = R.string.boot_err_unknown_title) + "\n\n" + stringResource(id = R.string.boot_err_unknown_detail),
                textFirstButton = stringResource(id = R.string.view_logs),
                onClickFirstButton = onClickViewLogs,
                textSecondButton = stringResource(id = R.string.mreturn),
                onClickSecondButton = onClickClear,
                showError = true,
                suggestion = stringResource(id = R.string.boot_err_unknown_suggestion),
            )
    }
}
