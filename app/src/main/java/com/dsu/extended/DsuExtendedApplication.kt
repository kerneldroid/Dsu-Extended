package com.dsu.extended

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.dsu.extended.di.ServiceEntryPoint
import com.dsu.extended.preferences.AppPrefs
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class DsuExtendedApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        // Per-process exemption. Must live here, not in an Activity: widget /
        // broadcast cold starts never create an Activity, and without this
        // hidden APIs (e.g. SystemProperties in the widget) throw.
        HiddenApiBypass.addHiddenApiExemptions("")
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        clearStaleInstallFlag()
        applyAutoRuOrEnLocale()
    }

    /**
     * A previous process may have died mid-install (LMK/OOM): the install job
     * is gone with it, so a persisted "in progress" flag would wedge the
     * widget into a non-clickable state forever. Fresh process = nothing is
     * installing.
     */
    private fun clearStaleInstallFlag() {
        try {
            val entry = EntryPointAccessors.fromApplication(
                this,
                ServiceEntryPoint::class.java,
            )
            val dataStore = entry.preferencesDataStore()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                runCatching {
                    dataStore.edit { prefs ->
                        prefs[booleanPreferencesKey(AppPrefs.INSTALLATION_IN_PROGRESS)] = false
                    }
                }
            }
        } catch (_: Exception) {
            // Hilt/DataStore not ready yet: HomeViewModel init covers it later.
        }
    }

    private fun applyAutoRuOrEnLocale() {
        val systemLocale = LocaleListCompat.getAdjustedDefault().get(0)
        val targetLanguage = if (systemLocale?.language?.lowercase(Locale.ROOT) == "ru") "ru" else "en"
        val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(",")
        if (currentLanguage != targetLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetLanguage))
        }
    }
}
