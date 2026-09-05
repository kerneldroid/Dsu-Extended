package com.dsu.extended.widget

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontFamily
import com.dsu.extended.di.WidgetDataStoreEntryPoint
import com.dsu.extended.preferences.AppPrefs
import com.dsu.extended.ui.theme.AppFontPreset
import com.dsu.extended.ui.theme.ColorPaletteStyle
import com.dsu.extended.ui.theme.ColorSpecVersion
import com.dsu.extended.ui.theme.Primary
import com.dsu.extended.ui.theme.ThemeMode
import com.dsu.extended.ui.theme.materialColorScheme
import com.dsu.extended.ui.theme.toOledSurfaceScheme
import dagger.hilt.android.EntryPointAccessors

object DsuWidgetTheme {
    // The Hilt singleton: a second DataStore on the same file throws
    // IllegalStateException and would silently reset the widget to defaults.
    fun sharedDataStore(context: Context): DataStore<Preferences> =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDataStoreEntryPoint::class.java,
        ).preferencesDataStore()

    fun getAppWidgetColorProviders(context: Context, prefs: Preferences?): ColorProviders {
        val colorStyle = ColorPaletteStyle.fromPreference(
            prefs?.get(stringPreferencesKey(AppPrefs.MATERIAL_COLOR_STYLE))
                ?: ColorPaletteStyle.TONAL_SPOT.value,
        )
        val colorSpec = ColorSpecVersion.fromPreference(
            prefs?.get(stringPreferencesKey(AppPrefs.MATERIAL_COLOR_SPEC))
                ?: ColorSpecVersion.SPEC_2021.value,
        )
        val useDynamic = prefs?.get(booleanPreferencesKey(AppPrefs.USE_DYNAMIC_COLOR)) ?: false
        val themeMode = ThemeMode.fromPreference(
            prefs?.get(stringPreferencesKey(AppPrefs.THEME_MODE))
                ?: ThemeMode.SYSTEM.value,
        )

        val hasDynamic = useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        // dynamicLight/DarkColorScheme(Context) are plain functions, safe here.
        val systemDynamicLight = if (hasDynamic) {
            runCatching { dynamicLightColorScheme(context) }.getOrNull()
        } else {
            null
        }

        // In the widget worker WallpaperManager often returns null; fall back
        // to the system Monet seed exactly like the app does, not to blue.
        val dynamicSeed = if (hasDynamic) {
            runCatching {
                val wp = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                val seed = wp?.primaryColor ?: wp?.secondaryColor ?: wp?.tertiaryColor
                seed?.toArgb()?.let { Color(it) }
            }.getOrNull() ?: systemDynamicLight?.primary ?: Primary
        } else {
            Primary
        }

        val lightScheme = if (hasDynamic && colorStyle == ColorPaletteStyle.TONAL_SPOT && systemDynamicLight != null) {
            systemDynamicLight
        } else {
            materialColorScheme(
                seedColor = dynamicSeed,
                useDarkTheme = false,
                colorPaletteStyle = colorStyle,
                colorSpecVersion = colorSpec,
                specVersion = colorSpec,
            )
        }

        val darkScheme = if (hasDynamic && colorStyle == ColorPaletteStyle.TONAL_SPOT) {
            runCatching { dynamicDarkColorScheme(context) }.getOrNull() ?: materialColorScheme(
                seedColor = dynamicSeed,
                useDarkTheme = true,
                colorPaletteStyle = colorStyle,
                colorSpecVersion = colorSpec,
                specVersion = colorSpec,
            )
        } else {
            materialColorScheme(
                seedColor = dynamicSeed,
                useDarkTheme = true,
                colorPaletteStyle = colorStyle,
                colorSpecVersion = colorSpec,
                specVersion = colorSpec,
            )
        }

        // Glance resolves day/night by the HOST night mode, ignoring in-app
        // overrides. To honor the app theme mode, pin both slots to one scheme.
        return when (themeMode) {
            ThemeMode.LIGHT -> ColorProviders(lightScheme, lightScheme)
            ThemeMode.DARK -> ColorProviders(darkScheme, darkScheme)
            ThemeMode.OLED -> {
                val oled = darkScheme.toOledSurfaceScheme()
                ColorProviders(oled, oled)
            }
            ThemeMode.SYSTEM -> ColorProviders(lightScheme, darkScheme)
        }
    }

    /**
     * RemoteViews cannot use the app's downloadable fonts (Google Sans Flex/Code),
     * only system families. Maps the app font preset to the closest Glance family;
     * null means Glance system default (matches SYSTEM_DEFAULT preset exactly).
     */
    fun getAppWidgetFontFamily(prefs: Preferences?): FontFamily? {
        val preset = AppFontPreset.fromPreference(
            prefs?.get(stringPreferencesKey(AppPrefs.APP_FONT_PRESET))
                ?: AppFontPreset.GOOGLE_SANS_FLEX.value,
        )
        return when (preset) {
            AppFontPreset.SYSTEM_DEFAULT -> null
            AppFontPreset.GOOGLE_SANS_FLEX -> FontFamily.SansSerif
            AppFontPreset.GOOGLE_SANS_CODE -> FontFamily.Monospace
        }
    }

    /**
     * The widget never probes DSU state itself (no shell, no system props —
     * both are unreliable/blocked from a widget worker). It only renders the
     * state published by the app layer (Session/HomeViewModel).
     */
    data class DsuWidgetState(
        val installed: Boolean,
        val running: Boolean,
        val installing: Boolean,
    )

    fun prefsToWidgetState(prefs: Preferences?): DsuWidgetState = DsuWidgetState(
        installed = prefs?.get(booleanPreferencesKey(AppPrefs.DSU_INSTALLED)) ?: false,
        running = prefs?.get(booleanPreferencesKey(AppPrefs.DSU_RUNNING)) ?: false,
        installing = prefs?.get(booleanPreferencesKey(AppPrefs.INSTALLATION_IN_PROGRESS)) ?: false,
    )
}
