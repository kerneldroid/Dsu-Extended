package com.dsu.extended.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.dsu.extended.di.ServiceEntryPoint
import com.dsu.extended.installer.root.DSUInstaller
import com.dsu.extended.preferences.AppPrefs
import com.dsu.extended.preparation.InstallationStep
import com.dsu.extended.util.AppLogger
import com.dsu.extended.util.InstallationLiveUpdateNotifier
import com.dsu.extended.widget.DsuAppWidget
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll

/**
 * Hosts root GSI flashing outside the UI layer. The install coroutine lives
 * in [serviceScope], so Activity destroy/recreate cannot tear it down; only
 * explicit cancel or process death stops it. Progress reaches the UI through
 * [InstallStatusBus]; terminal state is persisted here too, so headless
 * completion still updates prefs, notifications and the widget.
 */
class DsuInstallService : Service() {

    companion object {
        const val ACTION_START_ROOT_INSTALL = "com.dsu.extended.action.START_ROOT_INSTALL"
        const val ACTION_CANCEL_INSTALL = "com.dsu.extended.action.CANCEL_INSTALL"

        fun startIntent(context: Context): Intent =
            Intent(context, DsuInstallService::class.java).setAction(ACTION_START_ROOT_INSTALL)

        fun cancelIntent(context: Context): Intent =
            Intent(context, DsuInstallService::class.java).setAction(ACTION_CANCEL_INSTALL)
    }

    private val tag = this.javaClass.simpleName
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var installJob: Job? = null
    private lateinit var notifier: InstallationLiveUpdateNotifier

    // Last known trio for notification refreshes on step-only callbacks.
    private var lastStep: InstallationStep = InstallationStep.PROCESSING
    private var lastProgress: Float = 0f
    private var lastPartition: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifier = InstallationLiveUpdateNotifier(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_ROOT_INSTALL -> startRootInstall()
            ACTION_CANCEL_INSTALL -> {
                AppLogger.i(tag, "Cancel requested")
                installJob?.cancel()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        installJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startRootInstall() {
        if (installJob?.isActive == true) {
            AppLogger.w(tag, "Install already running, ignoring duplicate start")
            return
        }
        startForeground(
            InstallationLiveUpdateNotifier.NOTIFICATION_ID,
            notifier.buildProgressNotification(InstallationStep.PROCESSING, 0f, ""),
        )
        installJob = serviceScope.launch {
            var terminal: Terminal? = null
            try {
                val entry = EntryPointAccessors.fromApplication(
                    applicationContext,
                    ServiceEntryPoint::class.java,
                )
                val session = entry.session()
                DSUInstaller(
                    application = application,
                    userdataSize = session.userSelection.userSelectedUserdata,
                    dsuInstallation = session.dsuInstallation,
                    installationJob = coroutineContext[Job]!!,
                    onInstallationError = { step, text ->
                        terminal = Terminal.Error(step, text)
                        notifier.showError(text)
                        InstallStatusBus.emit(InstallStatusBus.Event.TerminalError(step, text))
                    },
                    onInstallationProgressUpdate = { progress, partition ->
                        lastProgress = progress
                        lastPartition = partition
                        notifier.showProgress(lastStep, progress, partition)
                        InstallStatusBus.emit(InstallStatusBus.Event.Progress(progress, partition))
                    },
                    onCreatePartition = { partition ->
                        lastPartition = partition
                        notifier.showProgress(lastStep, lastProgress, partition)
                        InstallStatusBus.emit(InstallStatusBus.Event.Partition(partition))
                    },
                    onInstallationStepUpdate = { step ->
                        lastStep = step
                        notifier.showProgress(step, lastProgress, lastPartition)
                        InstallStatusBus.emit(InstallStatusBus.Event.Step(step))
                    },
                    onInstallationSuccess = {
                        terminal = Terminal.Success
                        notifier.showSuccess(canRebootToDsu = true)
                        InstallStatusBus.emit(InstallStatusBus.Event.TerminalSuccess(rooted = true))
                    },
                ).invoke()
            } finally {
                persistTerminalState(terminal)
                stopSelf()
            }
        }
    }

    private sealed interface Terminal {
        data object Success : Terminal
        data class Error(val step: InstallationStep, val text: String) : Terminal
    }

    private suspend fun persistTerminalState(terminal: Terminal?) {
        runCatching {
            val entry = EntryPointAccessors.fromApplication(
                applicationContext,
                ServiceEntryPoint::class.java,
            )
            val dataStore = entry.preferencesDataStore()
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(AppPrefs.INSTALLATION_IN_PROGRESS)] = false
                if (terminal is Terminal.Success) {
                    prefs[booleanPreferencesKey(AppPrefs.DSU_INSTALLED)] = true
                    prefs[booleanPreferencesKey(AppPrefs.DSU_RUNNING)] = false
                }
            }
            DsuAppWidget().updateAll(applicationContext)
        }.onFailure {
            AppLogger.w(tag, "Failed to persist terminal install state", "error" to (it.message ?: "unknown"))
        }
    }
}
