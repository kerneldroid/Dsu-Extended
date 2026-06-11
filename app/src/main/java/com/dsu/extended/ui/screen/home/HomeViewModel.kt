package com.dsu.extended.ui.screen.home

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dsu.extended.BuildConfig
import com.dsu.extended.R
import com.dsu.extended.core.BaseViewModel
import com.dsu.extended.core.StorageManager
import com.dsu.extended.installer.adb.AdbInstallationHandler
import com.dsu.extended.installer.privileged.DsuInstallationHandler
import com.dsu.extended.installer.privileged.LogcatDiagnostic
import com.dsu.extended.installer.root.DSUInstaller
import com.dsu.extended.model.DSUInstallationSource
import com.dsu.extended.model.Session
import com.dsu.extended.preferences.AppPrefs
import com.dsu.extended.preparation.InstallationStep
import com.dsu.extended.preparation.Preparation
import com.dsu.extended.service.PrivilegedProvider
import com.dsu.extended.util.DevicePropUtils
import com.dsu.extended.util.FilenameUtils
import com.dsu.extended.util.OperationMode
import com.dsu.extended.util.OperationModeUtils
import com.dsu.extended.util.LogsStore
import com.dsu.extended.util.StoredLogType
import com.dsu.extended.util.StorageUtils
import com.dsu.extended.util.AppLogger
import com.dsu.extended.util.InstallationLiveUpdateNotifier

@HiltViewModel
class HomeViewModel @Inject constructor(
    val application: Application,
    override val dataStore: DataStore<Preferences>,
    private val storageManager: StorageManager,
    var session: Session,
) : BaseViewModel(dataStore) {

    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    var checkDynamicPartitions = true
    var checkUnavailableStorage = true
    var checkReadLogsPermission = true
    var disabledStorageCheck = false

    var installationJob: Job = Job()
    private var logger: LogcatDiagnostic? = null
    private val liveUpdateNotifier = InstallationLiveUpdateNotifier(application.applicationContext)

    private val allocPercentage = DevicePropUtils.getGsidBinaryAllowedPerc()
    val allocPercentageInt = String.format("%.0f", allocPercentage * 100).toInt()

    private val storageStats = StorageUtils.getAllocInfo(allocPercentage)
    private val hasAvailableStorage = storageStats.first
    private val maximumAllowedForAllocation = storageStats.second

    //
    // Helper methods used for controlling UI State
    //

    private fun updateAdditionalCardState(additionalCard: AdditionalCardState) =
        _uiState.update { it.copy(additionalCard = additionalCard) }

    private fun updateUserdataCard(update: (UserDataCardState) -> UserDataCardState) =
        _uiState.update { it.copy(userDataCard = update(it.userDataCard.copy())) }

    private fun updateInstallationCard(update: (InstallationCardState) -> InstallationCardState) =
        _uiState.update { it.copy(installationCard = update(it.installationCard.copy())) }

    private fun updateImageSizeCard(update: (ImageSizeCardState) -> ImageSizeCardState) =
        _uiState.update { it.copy(imageSizeCard = update(it.imageSizeCard.copy())) }

    private fun updateSheetState(sheetDisplay: SheetDisplayState) =
        _uiState.update { it.copy(sheetDisplay = sheetDisplay) }

    fun resetInstallationCard() {
        liveUpdateNotifier.cancel()
        _uiState.update {
            it.copy(
                installationCard = InstallationCardState(),
                sheetDisplay = SheetDisplayState.NONE,
            )
        }
    }

    fun dismissSheet() = updateSheetState(SheetDisplayState.NONE)

    //
    // Home startup and checks
    //

    init {
        // Check if a DSU is already installed
        // Root-only because MANAGE_DYNAMIC_SYSTEM is required
        if (session.isRoot()) {
            PrivilegedProvider.run {
                if (isInUse) {
                    updateInstallationCard { it.copy(installationStep = InstallationStep.DSU_ALREADY_RUNNING_DYN_OS) }
                    return@run
                }
                if (isInstalled) {
                    updateInstallationCard { it.copy(installationStep = InstallationStep.DSU_ALREADY_INSTALLED) }
                    return@run
                }
            }
        }
    }

    fun initialChecks() {
        AppLogger.i(
            tag,
            "Running initial checks",
            "checkDynamicPartitions" to checkDynamicPartitions,
            "checkUnavailableStorage" to checkUnavailableStorage,
            "hasAvailableStorage" to hasAvailableStorage,
            "allocPercentage" to allocPercentage,
            "maximumAllowedForAllocation" to maximumAllowedForAllocation,
        )

        if (checkDynamicPartitions && !DevicePropUtils.hasDynamicPartitions()) {
            AppLogger.w(tag, "Initial check failed: missing dynamic partitions")
            updateAdditionalCardState(AdditionalCardState.NO_DYNAMIC_PARTITIONS)
            return
        }

        if (checkUnavailableStorage && !hasAvailableStorage) {
            AppLogger.w(
                tag,
                "Initial check failed: unavailable storage",
                "hasAvailableStorage" to hasAvailableStorage,
                "allocPercentage" to allocPercentage,
                "maximumAllowedForAllocation" to maximumAllowedForAllocation,
            )
            updateAdditionalCardState(AdditionalCardState.UNAVAILABLE_STORAGE)
            return
        }

        viewModelScope.launch {
            val result = readStringPref(AppPrefs.SAF_PATH)
            if (!storageManager.arePermissionsGrantedToFolder(result)) {
                updateAdditionalCardState(AdditionalCardState.SETUP_STORAGE)
                return@launch
            }

            if ((session.getOperationMode() == OperationMode.SHIZUKU || session.getOperationMode() == OperationMode.DHIZUKU) &&
                checkReadLogsPermission &&
                !OperationModeUtils.isReadLogsPermissionGranted(application)
            ) {
                _uiState.update { it.copy(passedInitialChecks = false) }
                updateAdditionalCardState(AdditionalCardState.MISSING_READ_LOGS_PERMISSION)
                return@launch
            }

            val seenUnlockedBootloaderWarning = readBoolPref(AppPrefs.BOOTLOADER_UNLOCKED_WARNING)
            if (!seenUnlockedBootloaderWarning) {
                _uiState.update { it.copy(passedInitialChecks = false) }
                updateAdditionalCardState(AdditionalCardState.BOOTLOADER_UNLOCKED_WARNING)
                return@launch
            }

            updateAdditionalCardState(AdditionalCardState.NONE)
            _uiState.update { it.copy(passedInitialChecks = true) }
            AppLogger.i(tag, "Initial checks passed")
        }
    }

    fun setupUserPreferences() {
        viewModelScope.launch {
            val shouldKeepScreenOn = readBoolPref(AppPrefs.KEEP_SCREEN_ON)
            AppLogger.d(tag, "Preference loaded", "name" to AppPrefs.KEEP_SCREEN_ON, "value" to shouldKeepScreenOn)
            _uiState.update { it.copy(shouldKeepScreenOn = shouldKeepScreenOn) }

            disabledStorageCheck = readBoolPref(AppPrefs.DISABLE_STORAGE_CHECK)
            AppLogger.d(tag, "Preference loaded", "name" to AppPrefs.DISABLE_STORAGE_CHECK, "value" to disabledStorageCheck)
        }
    }

    fun overrideDynamicPartitionCheck() {
        checkDynamicPartitions = false
        AppLogger.w(tag, "Dynamic partition check overridden", "checkDynamicPartitions" to checkDynamicPartitions)
        initialChecks()
    }

    fun overrideUnavailableStorage() {
        checkUnavailableStorage = false
        AppLogger.w(tag, "Storage availability check overridden", "checkUnavailableStorage" to checkUnavailableStorage)
        initialChecks()
    }

    fun onClickBootloaderUnlockedWarning() {
        viewModelScope.launch {
            updateBoolPref(AppPrefs.BOOTLOADER_UNLOCKED_WARNING, true)
            initialChecks()
        }
    }

    //
    // Installation
    //

    fun obtainSelectedFilename(): String = session.userSelection.selectedFileName

    fun onClickCancel() {
        if (uiState.value.isInstalling()) {
            updateSheetState(SheetDisplayState.CANCEL_INSTALLATION)
        }
    }

    fun onClickInstall() {
        session.userSelection.setUserDataSize(uiState.value.userDataCard.text)
        session.userSelection.setImageSize(uiState.value.imageSizeCard.text)
        updateSheetState(SheetDisplayState.CONFIRM_INSTALLATION)
    }

    fun onConfirmInstallationSheet() {
        dismissSheet()
        installationJob = Job()
        viewModelScope.launch(Dispatchers.IO + installationJob) {
            updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING, installationProgress = 0f) }
            liveUpdateNotifier.showProgress(
                step = InstallationStep.PROCESSING,
                progress = 0f,
                partition = "",
            )
            session.preferences.isUnmountSdCard = readBoolPref(AppPrefs.UMOUNT_SD)
            session.preferences.useBuiltinInstaller = readBoolPref(AppPrefs.USE_BUILTIN_INSTALLER)
            Preparation(
                storageManager = storageManager,
                session = session,
                job = installationJob,
                onStepUpdate = this@HomeViewModel::onStepUpdate,
                onPreparationProgressUpdate = this@HomeViewModel::onPreparationProgressUpdate,
                onCanceled = this@HomeViewModel::onClickCancelInstallationButton,
                onPreparationFinished = this@HomeViewModel::onPreparationFinished,
            ).invoke()
        }
    }

    private fun onPreparationFinished(dsuInstallation: DSUInstallationSource) {
        AppLogger.i(tag, "Preparation finished", "sourceType" to dsuInstallation.type, "uri" to dsuInstallation.uri)
        session.dsuInstallation = dsuInstallation
        startInstallation()
    }

    private fun startInstallation() {
        AppLogger.i(
            tag,
            "Starting installation",
            "mode" to session.getOperationMode(),
            "useBuiltinInstaller" to session.preferences.useBuiltinInstaller,
            "isRoot" to session.isRoot(),
        )
        updateInstallationCard {
            it.copy(
                installationStep = InstallationStep.PROCESSING,
                installationProgress = 0f,
                currentPartitionText = "",
            )
        }
        persistAutoModeSnapshot("start_installation")

        if (session.getOperationMode() == OperationMode.ADB) {
            setupAdbInstallation()
            return
        }

        if (session.preferences.useBuiltinInstaller && session.isRoot()) {
            startDSUInstallation()
            return
        }

        startPrivilegedInstallation()
    }

    private fun setupAdbInstallation() {
        AdbInstallationHandler(storageManager, session).generate { scriptPath ->
            AppLogger.i(tag, "ADB installation script generated", "scriptPath" to scriptPath)
            session.installationScriptPath = scriptPath
            liveUpdateNotifier.cancel()
            resetInstallationCard()
            updateInstallationCard { it.copy(installationStep = InstallationStep.REQUIRES_ADB_CMD_TO_CONTINUE) }
        }
    }

    private fun startDSUInstallation() {
        DSUInstaller(
            application = application,
            userdataSize = session.userSelection.userSelectedUserdata,
            dsuInstallation = session.dsuInstallation,
            installationJob = installationJob,
            onInstallationError = this::onInstallationError,
            onInstallationProgressUpdate = this::onInstallationProgressUpdate,
            onCreatePartition = this::onCreatePartition,
            onInstallationStepUpdate = this::onStepUpdate,
            onInstallationSuccess = this::onRootInstallationSuccess,
        ).invoke()
    }

    private fun startPrivilegedInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.WAITING_USER_CONFIRMATION) }
        liveUpdateNotifier.showProgress(
            step = InstallationStep.WAITING_USER_CONFIRMATION,
            progress = 0f,
            partition = "",
        )
        AppLogger.i(tag, "Forwarding install to privileged service", "mode" to session.getOperationMode())
        DsuInstallationHandler(session).startInstallation()
        if (session.isRoot() || OperationModeUtils.isReadLogsPermissionGranted(application)) {
            startLogging()
        } else {
            AppLogger.w(tag, "Read logs unavailable, live progress tracking disabled")
        }
    }

    // Track and diagnose installation by reading logcat
    private fun startLogging() {
        if (logger == null) {
            logger = LogcatDiagnostic(
                onInstallationError = this::onInstallationError,
                onStepUpdate = this::onStepUpdate,
                onInstallationProgressUpdate = this::onInstallationProgressUpdate,
                onInstallationSuccess = this::onInstallationSuccess,
                onLogLineReceived = this::onLogLineReceived,
            )
        }
        viewModelScope.launch(Dispatchers.IO + installationJob) {
            val currentLogger = logger ?: return@launch
            currentLogger.shouldLogEverything = readBoolPref(AppPrefs.FULL_LOGCAT_LOGGING)
            AppLogger.i(
                tag,
                "Logcat diagnostic started",
                "fullLogcat" to currentLogger.shouldLogEverything,
                "mode" to session.getOperationMode(),
            )
            currentLogger.startLogging(generateUsefulLogInfo())
        }
    }

    private fun generateUsefulLogInfo(): String {
        return "Device: ${Build.MODEL}\n" +
            "SDK: Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})\n" +
            "$session\n" +
            "Package: ${BuildConfig.APPLICATION_ID}\n" +
            "Version: ${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE})\n" +
            "checkDynamicPartitions: $checkDynamicPartitions\n" +
            "checkUnavailableStorage: $checkUnavailableStorage\n" +
            "checkReadLogsPermission: $checkReadLogsPermission\n" +
            "allocPercentage: $allocPercentage\n" +
            "hasAvailableStorage: $hasAvailableStorage\n" +
            "maximumAllowedForAllocation: $maximumAllowedForAllocation\n"
    }

    fun saveLogs(uriToSaveLogs: Uri) {
        val currentUiState = uiState.value
        val configSnapshot = buildConfigSnapshot()
        val entries =
            linkedMapOf(
                "filtered-logcat.txt" to currentUiState.installationLogs.ifBlank { "No filtered logcat captured." },
                "app-events.txt" to AppLogger.dumpBufferedLogs().ifBlank { "No in-app logs captured." },
                "config-snapshot.txt" to configSnapshot,
            )
        AppLogger.i(
            tag,
            "Saving logs archive",
            "uri" to uriToSaveLogs,
            "entries" to entries.keys.joinToString(","),
            "filteredLogLength" to currentUiState.installationLogs.length,
        )
        storageManager.writeZipToUri(entries, uriToSaveLogs)
    }

    fun onClickCancelInstallationButton() {
        persistInstallationSnapshot("installation_canceled")
        resetInstallationCard()
        if (session.getOperationMode() != OperationMode.ADB && logger?.isLogging?.get() == true) {
            AppLogger.w(tag, "Cancelling active installation", "mode" to session.getOperationMode())
            logger?.destroy()
            PrivilegedProvider.run { forceStopPackage("com.android.dynsystem") }
        }

        if (installationJob.isActive) {
            installationJob.cancel()
        }
        session.dsuInstallation = DSUInstallationSource()
    }

    //
    // Installation Card actions
    //

    fun onClickRebootToDynOS() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            setEnable(true, true)
            Shell.cmd("reboot").exec()
        }
    }

    fun onClickDiscardGsiAndStartInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            remove()
            forceStopPackage("com.android.dynsystem")
            startDSUInstallation()
        }
    }

    fun onClickDiscardGsi() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        PrivilegedProvider.run {
            remove()
            forceStopPackage("com.android.dynsystem")
            dismissSheet()
            resetInstallationCard()
        }
    }

    fun onClickRetryInstallation() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        startInstallation()
    }

    fun onClickUnmountSdCardAndRetry() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        session.preferences.isUnmountSdCard = true
        startInstallation()
    }

    fun onClickSetSeLinuxPermissive() {
        updateInstallationCard { it.copy(installationStep = InstallationStep.PROCESSING) }
        viewModelScope.launch {
            Shell.cmd("setenforce 0").exec()
            delay(5000)
            startInstallation()
        }
    }

    fun showDiscardSheet() = updateSheetState(SheetDisplayState.DISCARD_DSU)

    //
    // Userdata card
    //

    fun onCheckUserdataCard() =
        updateUserdataCard { it.copy(isSelected = !it.isSelected, text = "") }

    fun updateUserdataSize(input: String) {
        val selectedSize = FilenameUtils.getDigits(input)
        val sizeWithSuffix = FilenameUtils.appendToDigitsToString(input, "GB")
        AppLogger.d(
            tag,
            "Userdata size changed",
            "disabledStorageCheck" to disabledStorageCheck,
            "selectedSize" to selectedSize,
            "maximumAllowedForAllocation" to maximumAllowedForAllocation,
        )

        if (!disabledStorageCheck && selectedSize.isNotEmpty() && selectedSize.toInt() > maximumAllowedForAllocation) {
            val fixedSize =
                FilenameUtils.appendToDigitsToString("$maximumAllowedForAllocation", "GB")
            updateUserdataCard {
                it.copy(
                    text = fixedSize,
                    isError = true,
                    maximumAllowed = maximumAllowedForAllocation,
                )
            }
            viewModelScope.launch {
                delay(5000)
                updateUserdataCard { it.copy(isError = false) }
            }
            return
        }

        updateUserdataCard { it.copy(text = sizeWithSuffix) }
    }

    //
    // Image size card
    //

    fun onCheckImageSizeCard() {
        if (!uiState.value.imageSizeCard.isSelected) {
            updateSheetState(SheetDisplayState.IMAGESIZE_WARNING)
        } else {
            dismissSheet()
        }
        updateImageSizeCard { it.copy(isSelected = !it.isSelected, text = "") }
    }

    fun updateImageSize(input: String) {
        val inputWithSuffix = FilenameUtils.appendToDigitsToString(input, "b")
        updateImageSizeCard { it.copy(text = inputWithSuffix) }
    }

    //
    // File selection
    //

    fun takeUriPermission(uri: Uri) {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        viewModelScope.launch {
            if (storageManager.arePermissionsGrantedToFolder(uri.toString())) {
                updateStringPref(AppPrefs.SAF_PATH, uri.toString()) { initialChecks() }
            }
        }
    }

    fun onFileSelectionResult(uri: Uri) {
        val filename = FilenameUtils.queryName(application.contentResolver, uri)
        val extension = filename.substringAfterLast(".", "")
        val supportedFiles = arrayListOf("gz", "xz", "img", "gzip")

        // DSU packages (zip files), are only supported in R+
        if (Build.VERSION.SDK_INT > 29) {
            supportedFiles.add("zip")
        }
        val isFileSupported = supportedFiles.contains(extension)
        AppLogger.i(
            tag,
            "File selection result",
            "isFileSupported" to isFileSupported,
            "extension" to extension,
            "filename" to filename,
        )

        if (!isFileSupported) {
            viewModelScope.launch {
                updateInstallationCard { it.copy(isError = true, isTextFieldEnabled = false) }
                delay(2000)
                updateInstallationCard { it.copy(isError = false, isTextFieldEnabled = true) }
            }
            return
        }

        session.userSelection.selectedFileName = filename
        session.userSelection.selectedFileUri = uri
        updateInstallationCard {
            it.copy(
                text = filename,
                isTextFieldEnabled = false,
                isInstallable = true,
            )
        }
    }

    //
    // Read logs permission warning
    //

    fun grantReadLogs() {
        updateAdditionalCardState(AdditionalCardState.GRANTING_READ_LOGS_PERMISSION)
        val intent = Intent()
        intent.setClassName(
            BuildConfig.APPLICATION_ID,
            "${BuildConfig.APPLICATION_ID}.MainActivity",
        )
        intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK
        PrivilegedProvider.run {
            grantPermission("android.permission.READ_LOGS")
            if (Build.VERSION.SDK_INT <= 30) {
                forceStopPackage(BuildConfig.APPLICATION_ID)
            }
            startActivity(intent)
        }
    }

    fun refuseReadLogs() {
        checkReadLogsPermission = false
        AppLogger.w(tag, "READ_LOGS permission request refused")
        initialChecks()
    }

    fun showLogsWarning() {
        updateSheetState(SheetDisplayState.VIEW_LOGS)
    }

    //
    // Progress tracking
    //

    private fun onRootInstallationSuccess() {
        persistInstallationSnapshot("installation_success_root")
        liveUpdateNotifier.showSuccess(canRebootToDsu = true)
        updateInstallationCard { it.copy(installationStep = InstallationStep.INSTALL_SUCCESS_REBOOT_DYN_OS) }
    }

    private fun onInstallationSuccess() {
        AppLogger.i(tag, "Installation marked as successful")
        persistInstallationSnapshot("installation_success")
        liveUpdateNotifier.showSuccess(canRebootToDsu = false)
        updateInstallationCard { it.copy(installationStep = InstallationStep.INSTALL_SUCCESS) }
    }

    private fun onLogLineReceived() {
        val currentLogger = logger ?: return
        _uiState.update { it.copy(installationLogs = currentLogger.logs) }
    }

    private fun onStepUpdate(step: InstallationStep) {
        updateInstallationCard { it.copy(installationStep = step) }
        val currentCardState = uiState.value.installationCard
        liveUpdateNotifier.showProgress(
            step = step,
            progress = currentCardState.installationProgress,
            partition = currentCardState.currentPartitionText,
        )
    }

    private fun onPreparationProgressUpdate(progress: Float) {
        val safeProgress = progress.coerceIn(0f, 1f)
        updateInstallationCard { it.copy(installationProgress = safeProgress) }
        val currentCardState = uiState.value.installationCard
        val progressStep =
            if (currentCardState.installationStep == InstallationStep.NOT_INSTALLING) {
                InstallationStep.PROCESSING
            } else {
                currentCardState.installationStep
            }
        liveUpdateNotifier.showProgress(
            step = progressStep,
            progress = safeProgress,
            partition = currentCardState.currentPartitionText,
        )
    }

    private fun onInstallationError(error: InstallationStep, errorContent: String) {
        AppLogger.e(tag, "Installation error received", null, "step" to error, "content" to errorContent)
        persistInstallationSnapshot("installation_error_${error.name.lowercase()}")
        updateInstallationCard {
            if (error == InstallationStep.ERROR_SELINUX && !session.isRoot()) {
                it.copy(
                    installationStep = InstallationStep.ERROR_SELINUX_ROOTLESS,
                    errorText = errorContent,
                )
            } else {
                it.copy(installationStep = error, errorText = errorContent)
            }
        }
        liveUpdateNotifier.showError(errorContent)
    }

    private fun onInstallationProgressUpdate(progress: Float, partition: String) {
        val currentStep = uiState.value.installationCard.installationStep
        val progressStep =
            when (currentStep) {
                InstallationStep.CREATING_PARTITION,
                InstallationStep.INSTALLING,
                InstallationStep.INSTALLING_ROOTED,
                InstallationStep.PROCESSING_LOG_READABLE,
                -> currentStep

                else -> InstallationStep.INSTALLING
            }
        val safeProgress = progress.coerceIn(0f, 1f)
        val normalizedProgress = safeProgress
        updateInstallationCard {
            it.copy(
                installationStep = progressStep,
                currentPartitionText = partition,
                installationProgress = normalizedProgress,
            )
        }
        liveUpdateNotifier.showProgress(
            step = progressStep,
            progress = normalizedProgress,
            partition = partition,
        )
    }

    private fun onCreatePartition(partition: String) {
        updateInstallationCard {
            it.copy(
                installationStep = InstallationStep.CREATING_PARTITION,
                currentPartitionText = partition,
                installationProgress = 0f,
            )
        }
        liveUpdateNotifier.showProgress(
            step = InstallationStep.CREATING_PARTITION,
            progress = 0f,
            partition = partition,
        )
    }

    private fun buildConfigSnapshot(): String {
        val state = uiState.value
        return buildString {
            appendLine(generateUsefulLogInfo())
            appendLine("selectedFileName=${session.userSelection.selectedFileName}")
            appendLine("selectedFileUri=${session.userSelection.selectedFileUri}")
            appendLine("sheetDisplay=${state.sheetDisplay}")
            appendLine("installationStep=${state.installationCard.installationStep}")
            appendLine("installationProgress=${state.installationCard.installationProgress}")
            appendLine("currentPartition=${state.installationCard.currentPartitionText}")
            appendLine("errorText=${state.installationCard.errorText}")
            appendLine("isInstallable=${state.installationCard.isInstallable}")
            appendLine("isInstalling=${state.isInstalling()}")
            appendLine("operationMode=${session.getOperationMode()}")
            appendLine("fullLogcatLogging=${logger?.shouldLogEverything}")
            appendLine("sdk=${Build.VERSION.SDK_INT}")
            appendLine("buildType=${BuildConfig.BUILD_TYPE}")
        }
    }

    private fun persistAutoModeSnapshot(reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val preferred = readStringPref(AppPrefs.OPERATION_MODE_OVERRIDE)
            val hasRoot = runCatching { Shell.getShell().isRoot }.getOrDefault(false)
            val hasShizuku = OperationModeUtils.isShizukuPermissionGranted(application)
            val hasDhizuku = OperationModeUtils.isDhizukuPermissionGranted(application)
            val hasSystemDsu = OperationModeUtils.isDsuPermissionGranted(application)
            val snapshot =
                buildString {
                    appendLine("reason=$reason")
                    appendLine("preferredMode=$preferred")
                    appendLine("resolvedMode=${session.getOperationMode()}")
                    appendLine("root=$hasRoot")
                    appendLine("shizuku=$hasShizuku")
                    appendLine("dhizuku=$hasDhizuku")
                    appendLine("systemDynamicPermission=$hasSystemDsu")
                    appendLine("readLogs=${OperationModeUtils.isReadLogsPermissionGranted(application)}")
                    appendLine("sdk=${Build.VERSION.SDK_INT}")
                    appendLine("device=${Build.MODEL}")
                    appendLine("time=${System.currentTimeMillis()}")
                }
            runCatching {
                LogsStore.writeLog(
                    context = application,
                    type = StoredLogType.AUTO_MODE,
                    title = "auto_mode_$reason",
                    content = snapshot,
                )
            }.onFailure {
                AppLogger.w(tag, "Failed to persist auto mode snapshot", "error" to it.message)
            }
        }
    }

    private fun persistInstallationSnapshot(reason: String) {
        val state = uiState.value
        val installationLog = state.installationLogs
        viewModelScope.launch(Dispatchers.IO) {
            val payload =
                buildString {
                    appendLine("reason=$reason")
                    appendLine("time=${System.currentTimeMillis()}")
                    appendLine()
                    appendLine("=== session snapshot ===")
                    appendLine(buildConfigSnapshot())
                    appendLine()
                    appendLine("=== installation logs ===")
                    appendLine(installationLog.ifBlank { "(empty)" })
                    appendLine()
                    appendLine("=== app events ===")
                    appendLine(AppLogger.dumpBufferedLogs().ifBlank { "(empty)" })
                }
            runCatching {
                LogsStore.writeLog(
                    context = application,
                    type = StoredLogType.INSTALLATION,
                    title = reason,
                    content = payload,
                )
            }.onFailure {
                AppLogger.w(tag, "Failed to persist installation snapshot", "error" to it.message)
            }
        }
    }
}
