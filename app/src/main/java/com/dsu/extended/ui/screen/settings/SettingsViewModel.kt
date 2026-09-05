package com.dsu.extended.ui.screen.settings

import android.app.Application
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dsu.extended.core.BaseViewModel
import com.dsu.extended.model.Session
import com.dsu.extended.preferences.AppPrefs
import com.dsu.extended.ui.theme.AppFontPreset
import com.dsu.extended.ui.theme.ColorPaletteStyle
import com.dsu.extended.ui.theme.ColorSpecVersion
import com.dsu.extended.ui.theme.ThemeMode
import com.dsu.extended.util.OperationMode
import com.dsu.extended.util.OperationModeUtils
import com.dsu.extended.util.PreferredPrivilegedMode
import com.dsu.extended.util.AppLogger
import com.dsu.extended.util.DataStoreUtils
import com.dsu.extended.widget.DsuAppWidget
import androidx.glance.appwidget.updateAll

private suspend fun probeHasRoot(): Boolean =
    withContext(Dispatchers.IO) {
        runCatching { Shell.getShell().isRoot }.getOrDefault(false)
    }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    override val dataStore: DataStore<Preferences>,
    private val session: Session,
    val application: Application,
) : BaseViewModel(dataStore) {

    private val tag = this.javaClass.simpleName

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun reloadPreferences() {
        uiState.value.preferences.forEach { entry ->
            viewModelScope.launch {
                val isEnabled = readBoolPref(entry.key)
                togglePreference(entry.key, isEnabled)
            }
        }

        viewModelScope.launch {
            val preferredMode = PreferredPrivilegedMode.fromPreference(readStringPref(AppPrefs.OPERATION_MODE_OVERRIDE))
            val appFontPreset = AppFontPreset.fromPreference(readStringPref(AppPrefs.APP_FONT_PRESET))
            val themeMode = ThemeMode.fromPreference(readStringPref(AppPrefs.THEME_MODE))
            val colorStyle = ColorPaletteStyle.fromPreference(readStringPref(AppPrefs.MATERIAL_COLOR_STYLE))
            val colorSpec = ColorSpecVersion.fromPreference(readStringPref(AppPrefs.MATERIAL_COLOR_SPEC))
            val dynamicColor = DataStoreUtils.readBoolPref(dataStore, AppPrefs.USE_DYNAMIC_COLOR, false)
            val hasRoot = probeHasRoot()
            val hasShizuku = OperationModeUtils.isShizukuPermissionGranted(application)
            val hasDhizuku = OperationModeUtils.isDhizukuPermissionGranted(application)
            val hasSystemDsu = OperationModeUtils.isDsuPermissionGranted(application)
            val canLoadGsiPrivileged = hasRoot || hasShizuku || hasDhizuku || hasSystemDsu

            _uiState.update {
                it.copy(
                    isRoot = session.isRoot(),
                    hasRootAccess = hasRoot,
                    hasShizukuAccess = hasShizuku,
                    hasDhizukuAccess = hasDhizuku,
                    canLoadGsiPrivileged = canLoadGsiPrivileged,
                    preferredPrivilegedMode = preferredMode,
                    appFontPreset = appFontPreset,
                    themeMode = themeMode,
                    useDynamicColor = dynamicColor,
                    colorPaletteStyle = colorStyle,
                    colorSpecVersion = colorSpec,
                    isDeveloperOptionsEnabled = readBoolPref(AppPrefs.DEVELOPER_OPTIONS)
                )
            }
        }
    }

    init {
        reloadPreferences()
    }

    fun togglePreference(preference: String, value: Boolean) {
        viewModelScope.launch {
            updateBoolPref(preference, value) {
                _uiState.update {
                    val cloneMap = hashMapOf<String, Boolean>()
                    cloneMap.putAll(uiState.value.preferences)
                    cloneMap[preference] = value
                    it.copy(preferences = cloneMap)
                }
            }
        }
    }

    fun openDialog(sheet: DialogSheetState) {
        _uiState.update { it.copy(dialogSheetState = sheet) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogSheetState = DialogSheetState.NONE) }
    }

    fun checkOperationMode(): String {
        return OperationModeUtils.getOperationModeAsString(session.getOperationMode())
    }

    fun getOperationMode(): OperationMode {
        return session.getOperationMode()
    }

    fun setPreferredPrivilegedMode(mode: PreferredPrivilegedMode) {
        viewModelScope.launch {
            updateStringPref(AppPrefs.OPERATION_MODE_OVERRIDE, mode.value)
            _uiState.update { it.copy(preferredPrivilegedMode = mode) }
            refreshPrivilegedChecks()
        }
    }

    fun refreshPrivilegedChecks() {
        viewModelScope.launch {
            val hasRoot = probeHasRoot()
            val hasShizuku = OperationModeUtils.isShizukuPermissionGranted(application)
            val hasDhizuku = OperationModeUtils.isDhizukuPermissionGranted(application)
            val hasSystemDsu = OperationModeUtils.isDsuPermissionGranted(application)
            val canLoadGsiPrivileged = hasRoot || hasShizuku || hasDhizuku || hasSystemDsu

            _uiState.update {
                it.copy(
                    isRoot = session.isRoot(),
                    hasRootAccess = hasRoot,
                    hasShizukuAccess = hasShizuku,
                    hasDhizukuAccess = hasDhizuku,
                    canLoadGsiPrivileged = canLoadGsiPrivileged,
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            updateStringPref(AppPrefs.THEME_MODE, mode.value)
            _uiState.update { it.copy(themeMode = mode) }
            refreshWidget()
        }
    }

    fun setAppFontPreset(preset: AppFontPreset) {
        viewModelScope.launch {
            updateStringPref(AppPrefs.APP_FONT_PRESET, preset.value)
            _uiState.update { it.copy(appFontPreset = preset) }
            refreshWidget()
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            updateBoolPref(AppPrefs.USE_DYNAMIC_COLOR, enabled)
            _uiState.update { it.copy(useDynamicColor = enabled) }
            refreshWidget()
        }
    }

    fun setColorPaletteStyle(style: ColorPaletteStyle) {
        viewModelScope.launch {
            updateStringPref(AppPrefs.MATERIAL_COLOR_STYLE, style.value)
            _uiState.update { it.copy(colorPaletteStyle = style) }
            refreshWidget()
        }
    }

    fun setColorSpecVersion(specVersion: ColorSpecVersion) {
        viewModelScope.launch {
            updateStringPref(AppPrefs.MATERIAL_COLOR_SPEC, specVersion.value)
            _uiState.update { it.copy(colorSpecVersion = specVersion) }
            refreshWidget()
        }
    }

    private suspend fun refreshWidget() {
        runCatching { DsuAppWidget().updateAll(application) }
    }

    fun checkAllStatusSummary(): String {
        val root = if (uiState.value.hasRootAccess) "Root" else ""
        val shizuku = if (uiState.value.hasShizukuAccess) "Shizuku" else ""
        val dhizuku = if (uiState.value.hasDhizukuAccess) "Dhizuku" else ""
        val available = listOf(root, shizuku, dhizuku).filter { it.isNotEmpty() }.joinToString(", ")
        return if (available.isEmpty()) "None available" else "Available via: $available"
    }

    fun runCheckAll() {
        refreshPrivilegedChecks()
    }

    fun setUseBuiltInInstaller(value: Boolean) {
        togglePreference(AppPrefs.USE_BUILTIN_INSTALLER, value)
    }

    fun setUnmountSdCard(value: Boolean) {
        togglePreference(AppPrefs.UMOUNT_SD, value)
    }

    fun setDisableStorageCheck(value: Boolean) {
        togglePreference(AppPrefs.DISABLE_STORAGE_CHECK, value)
    }

    fun setFullLogcatLogging(value: Boolean) {
        togglePreference(AppPrefs.FULL_LOGCAT_LOGGING, value)
    }

    fun setKeepScreenOn(value: Boolean) {
        togglePreference(AppPrefs.KEEP_SCREEN_ON, value)
    }
}
