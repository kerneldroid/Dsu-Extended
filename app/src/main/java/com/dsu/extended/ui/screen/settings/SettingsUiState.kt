package com.dsu.extended.ui.screen.settings

import com.dsu.extended.preferences.AppPrefs
import com.dsu.extended.ui.theme.AppFontPreset
import com.dsu.extended.ui.theme.ColorPaletteStyle
import com.dsu.extended.ui.theme.ThemeMode
import com.dsu.extended.util.PreferredPrivilegedMode

enum class DialogSheetState {
    NONE,
    BUILT_IN_INSTALLER,
    DISABLE_STORAGE_CHECK,
    UI_STYLE_SELECTOR,
    OPERATION_MODE_SELECTOR,
    THEME_MODE_SELECTOR,
    COLOR_STYLE_SELECTOR,
    FONT_SELECTOR,
}

data class SettingsUiState(
    val preferences: HashMap<String, Boolean> = hashMapOf(
        AppPrefs.USE_BUILTIN_INSTALLER to false,
        AppPrefs.KEEP_SCREEN_ON to false,
        AppPrefs.UMOUNT_SD to false,
        AppPrefs.DISABLE_STORAGE_CHECK to false,
        AppPrefs.FULL_LOGCAT_LOGGING to false,
    ),
    val dialogSheetState: DialogSheetState = DialogSheetState.NONE,
    val isRoot: Boolean = false,
    val hasRootAccess: Boolean = false,
    val hasShizukuAccess: Boolean = false,
    val hasDhizukuAccess: Boolean = false,
    val canLoadGsiPrivileged: Boolean = false,
    val isDeveloperOptionsEnabled: Boolean = false,
    val preferredPrivilegedMode: PreferredPrivilegedMode = PreferredPrivilegedMode.ALL,
    val appFontPreset: AppFontPreset = AppFontPreset.SYSTEM_DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val colorPaletteStyle: ColorPaletteStyle = ColorPaletteStyle.TONAL_SPOT,
) {
    val useBuiltInInstaller: Boolean
        get() = preferences[AppPrefs.USE_BUILTIN_INSTALLER] ?: false
    val unmountSdCard: Boolean
        get() = preferences[AppPrefs.UMOUNT_SD] ?: false
    val disableStorageCheck: Boolean
        get() = preferences[AppPrefs.DISABLE_STORAGE_CHECK] ?: false
    val keepScreenOn: Boolean
        get() = preferences[AppPrefs.KEEP_SCREEN_ON] ?: false
    val fullLogcatLogging: Boolean
        get() = preferences[AppPrefs.FULL_LOGCAT_LOGGING] ?: false
}
