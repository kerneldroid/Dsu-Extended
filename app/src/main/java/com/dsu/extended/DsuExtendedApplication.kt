package com.dsu.extended

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class DsuExtendedApplication : Application() {
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
