package com.dsu.extended.ui.screen.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.collectLatest
import com.dsu.extended.MainActivity
import com.dsu.extended.R
import com.dsu.extended.ui.components.AppScaffold
import com.dsu.extended.ui.cards.DsuInfoCard
import com.dsu.extended.ui.cards.ImageSizeCard
import com.dsu.extended.ui.cards.UserdataCard
import com.dsu.extended.ui.cards.installation.InstallationCard
import com.dsu.extended.ui.cards.warnings.GrantingPermissionCard
import com.dsu.extended.ui.cards.warnings.RequiresLogPermissionCard
import com.dsu.extended.ui.cards.warnings.SetupStorage
import com.dsu.extended.ui.cards.warnings.StorageWarningCard
import com.dsu.extended.ui.cards.warnings.UnlockedBootloaderCard
import com.dsu.extended.ui.cards.warnings.UnsupportedCard
import com.dsu.extended.ui.screen.Destinations
import com.dsu.extended.ui.sdialogs.CancelSheet
import com.dsu.extended.ui.sdialogs.ConfirmInstallationSheet
import com.dsu.extended.ui.sdialogs.DiscardDSUSheet
import com.dsu.extended.ui.sdialogs.ImageSizeWarningSheet
import com.dsu.extended.ui.sdialogs.ViewLogsBottomSheet
import com.dsu.extended.ui.util.KeepScreenOn
import com.dsu.extended.util.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon

object HomeLinks {
    const val DSU_LEARN_MORE = "https://developer.android.com/topic/dsu"
    const val DSU_DOCS = "https://source.android.com/devices/tech/ota/dynamic-system-updates"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(
    navigate: (String) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? MainActivity
    val uriHandler = LocalUriHandler.current

    if (uiState.shouldKeepScreenOn) {
        KeepScreenOn()
    }

    LaunchedEffect(Unit) {
        homeViewModel.setupUserPreferences()
        homeViewModel.session.operationMode.collectLatest {
            homeViewModel.initialChecks()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    AppScaffold(
        title = {
            Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleLarge)
        },
        actions = {
            IconButton(onClick = { navigate(Destinations.Preferences) }) {
                Icon(imageVector = Icons.Rounded.Settings, contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = if (uiState.additionalCard == AdditionalCardState.NONE) 0.dp else 10.dp)
                    .animateContentSize(),
            ) {
                when (uiState.additionalCard) {
                    AdditionalCardState.NO_DYNAMIC_PARTITIONS ->
                        UnsupportedCard(
                            onClickClose = { exitProcess(0) },
                            onClickContinueAnyway = { homeViewModel.overrideDynamicPartitionCheck() },
                        )

                    AdditionalCardState.SETUP_STORAGE ->
                        SetupStorage { homeViewModel.takeUriPermission(it) }

                    AdditionalCardState.UNAVAILABLE_STORAGE ->
                        StorageWarningCard(
                            minPercentageFreeStorage = homeViewModel.allocPercentageInt.toString(),
                            onClick = { homeViewModel.overrideUnavailableStorage() },
                        )

                    AdditionalCardState.MISSING_READ_LOGS_PERMISSION ->
                        RequiresLogPermissionCard(
                            onClickGrant = { homeViewModel.grantReadLogs() },
                            onClickRefuse = { homeViewModel.refuseReadLogs() },
                        )

                    AdditionalCardState.GRANTING_READ_LOGS_PERMISSION ->
                        GrantingPermissionCard()

                    AdditionalCardState.BOOTLOADER_UNLOCKED_WARNING ->
                        UnlockedBootloaderCard { homeViewModel.onClickBootloaderUnlockedWarning() }

                    AdditionalCardState.DEVICE_COMPATIBILITY_CHECK -> {}

                    AdditionalCardState.NONE -> {}
                }
            }
            if (uiState.passedInitialChecks && uiState.additionalCard == AdditionalCardState.NONE) {
                InstallationCard(
                    uiState = uiState.installationCard,
                    onClickInstall = {
                        activity?.requestPermissionsForCheckAll {
                            homeViewModel.onClickInstall()
                        } ?: homeViewModel.onClickInstall()
                    },
                    onClickUnmountSdCardAndRetry = { homeViewModel.onClickUnmountSdCardAndRetry() },
                    onClickSetSeLinuxPermissive = { homeViewModel.onClickSetSeLinuxPermissive() },
                    onClickRetryInstallation = { homeViewModel.onClickRetryInstallation() },
                    onClickClear = { homeViewModel.resetInstallationCard() },
                    onSelectFileSuccess = { homeViewModel.onFileSelectionResult(it) },
                    onClickCancelInstallation = { homeViewModel.onClickCancel() },
                    onClickDiscardInstalledGsiAndInstall = { homeViewModel.onClickDiscardGsiAndStartInstallation() },
                    onClickDiscardDsu = { homeViewModel.showDiscardSheet() },
                    onClickRebootToDynOS = { homeViewModel.onClickRebootToDynOS() },
                    onClickOpenLogsTab = { navigate(Destinations.Logs) },
                    onClickViewLogs = { homeViewModel.showLogsWarning() },
                    onClickViewCommands = { navigate(Destinations.ADBInstallation) },
                    minPercentageOfFreeStorage = homeViewModel.allocPercentageInt.toString(),
                )
                UserdataCard(
                    isEnabled = uiState.isInstalling(),
                    uiState = uiState.userDataCard,
                    onCheckedChange = { homeViewModel.onCheckUserdataCard() },
                    onValueChange = { homeViewModel.updateUserdataSize(it) },
                )
                ImageSizeCard(
                    isEnabled = uiState.isInstalling(),
                    uiState = uiState.imageSizeCard,
                    onCheckedChange = { homeViewModel.onCheckImageSizeCard() },
                    onValueChange = { homeViewModel.updateImageSize(it) },
                )
                DsuInfoCard(
                    onClickViewDocs = { uriHandler.openUri(HomeLinks.DSU_DOCS) },
                    onClickLearnMore = { uriHandler.openUri(HomeLinks.DSU_LEARN_MORE) },
                )
            }
        }
    }

    when (uiState.sheetDisplay) {
        SheetDisplayState.CONFIRM_INSTALLATION ->
            ConfirmInstallationSheet(
                filename = homeViewModel.obtainSelectedFilename(),
                userdata = homeViewModel.session.userSelection.getUserDataSizeAsGB(),
                fileSize = homeViewModel.session.userSelection.userSelectedImageSize,
                onClickConfirm = { homeViewModel.onConfirmInstallationSheet() },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.CANCEL_INSTALLATION ->
            CancelSheet(
                onClickConfirm = { homeViewModel.onClickCancelInstallationButton() },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.IMAGESIZE_WARNING ->
            ImageSizeWarningSheet(
                onClickConfirm = { homeViewModel.dismissSheet() },
                onClickCancel = { homeViewModel.onCheckImageSizeCard() },
            )

        SheetDisplayState.DISCARD_DSU ->
            DiscardDSUSheet(
                onClickConfirm = { homeViewModel.onClickDiscardGsi() },
                onClickCancel = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.VIEW_LOGS ->
            ViewLogsBottomSheet(
                logs = uiState.installationLogs,
                onClickSaveLogs = { homeViewModel.saveLogs(it) },
                onDismiss = { homeViewModel.dismissSheet() },
            )

        SheetDisplayState.DEVICE_INFO -> {}
        SheetDisplayState.DIAGNOSTIC_REPORT -> {}
        SheetDisplayState.INSTALLATION_HISTORY -> {}
        SheetDisplayState.NONE -> {}
    }
}
