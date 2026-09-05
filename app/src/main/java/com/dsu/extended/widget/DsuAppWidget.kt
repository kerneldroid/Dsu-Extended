package com.dsu.extended.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dsu.extended.MainActivity
import com.dsu.extended.R
import com.dsu.extended.preferences.AppPrefs
import com.dsu.extended.ui.theme.ThemeMode
import androidx.datastore.preferences.core.stringPreferencesKey

class DsuAppWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dataStore = DsuWidgetTheme.sharedDataStore(context)

        provideContent {
            val currentContext = LocalContext.current
            // Reactive DataStore subscription inside the Glance composition:
            // prefs changes recompose the widget without re-adding it.
            // updateAll() from the app still restarts provideGlance when no
            // session is running (official Glance pattern).
            val prefs by dataStore.data.collectAsState(initial = null)

            val widgetState = DsuWidgetTheme.prefsToWidgetState(prefs)
            val isInstalled = widgetState.installed
            val isRunning = widgetState.running
            val installing = widgetState.installing
            val isOled = (prefs?.get(stringPreferencesKey(AppPrefs.THEME_MODE)) ?: "") == ThemeMode.OLED.value

            val colorProviders = remember(prefs) {
                DsuWidgetTheme.getAppWidgetColorProviders(currentContext, prefs)
            }
            val fontFamily = remember(prefs) {
                DsuWidgetTheme.getAppWidgetFontFamily(prefs)
            }

            GlanceTheme(colors = colorProviders) {
                DsuWidgetLayout(
                    isInstalled = isInstalled,
                    isRunning = isRunning,
                    installing = installing,
                    isOled = isOled,
                    fontFamily = fontFamily,
                )
            }
        }
    }

    @Composable
    private fun DsuWidgetLayout(
        isInstalled: Boolean,
        isRunning: Boolean,
        installing: Boolean,
        isOled: Boolean,
        fontFamily: FontFamily?,
    ) {
        val context = LocalContext.current
        val colors = GlanceTheme.colors

        val badgeBg: Int
        val iconTint: Int
        val iconRes: Int
        val titleText: String
        val subtitleText: String

        when {
            installing -> {
                badgeBg = colors.secondaryContainer.getColor(context).toArgb()
                iconTint = colors.onSecondaryContainer.getColor(context).toArgb()
                iconRes = R.drawable.ic_nav_install
                titleText = "Installing GSI"
                subtitleText = "Do not close the app"
            }
            isRunning -> {
                badgeBg = colors.tertiary.getColor(context).toArgb()
                iconTint = colors.onTertiary.getColor(context).toArgb()
                iconRes = R.drawable.ic_nav_install_filled
                titleText = "Running DSU"
                subtitleText = "Dynamic System active"
            }
            isInstalled -> {
                badgeBg = colors.primary.getColor(context).toArgb()
                iconTint = colors.onPrimary.getColor(context).toArgb()
                iconRes = R.drawable.ic_nav_storage_filled
                titleText = "GSI Installed"
                subtitleText = "Ready to boot"
            }
            else -> {
                // Glance 1.1.1 has no surfaceContainerHighest token; use surfaceVariant.
                badgeBg = colors.surfaceVariant.getColor(context).toArgb()
                iconTint = colors.onSurfaceVariant.getColor(context).toArgb()
                iconRes = R.drawable.ic_nav_install
                titleText = "Not installed"
                subtitleText = "No GSI image staged"
            }
        }

        val badgeBitmap = runCatching {
            DsuWidgetBadge.createSunnyBadge(
                context = context,
                sizePx = 128,
                badgeColorArgb = badgeBg,
                iconRes = iconRes,
                iconTintArgb = iconTint,
            )
        }.getOrNull()

        // OLED: force true black root instead of the themed container.
        val backgroundModifier = if (isOled) {
            GlanceModifier.background(Color.Black)
        } else {
            GlanceModifier.background(colors.widgetBackground)
        }

        var boxModifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .then(backgroundModifier)
            .padding(16.dp)
        // No navigation actions while installing: relaunching MainActivity
        // mid-install tears down its ViewModel and aborts the install job.
        if (!installing) {
            boxModifier = boxModifier.clickable(actionStartActivity<MainActivity>())
        }

        Box(
            modifier = boxModifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (badgeBitmap != null) {
                        Image(
                            provider = ImageProvider(badgeBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.size(44.dp),
                        )

                        Spacer(modifier = GlanceModifier.width(12.dp))
                    }

                    Column {
                        Text(
                            text = titleText,
                            style = TextStyle(
                                color = colors.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily,
                            ),
                        )
                        Text(
                            text = subtitleText,
                            style = TextStyle(
                                color = colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontFamily = fontFamily,
                            ),
                            maxLines = 1,
                        )
                    }
                }

                if (isInstalled && !isRunning && !installing) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Button(
                        text = "Reboot",
                        onClick = actionRunCallback<RebootToDsuActionCallback>(),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .cornerRadius(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colors.primary,
                            contentColor = colors.onPrimary,
                        ),
                    )
                }
            }
        }
    }
}

class DsuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DsuAppWidget()
}
