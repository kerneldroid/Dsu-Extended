package com.dsu.extended

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
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
        applyAutoRuOrEnLocale()
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
