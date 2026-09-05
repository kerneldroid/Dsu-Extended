package com.dsu.extended.core

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.PowerManager
import com.dsu.extended.service.PrivilegedProvider
import com.dsu.extended.util.AppLogger
import com.dsu.extended.util.OperationMode
import com.dsu.extended.util.OperationModeUtils
import com.rosan.dhizuku.api.Dhizuku
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object DsuRebootManager {
    private const val TAG = "DsuRebootManager"

    suspend fun rebootToDsu(context: Context): Boolean = withContext(Dispatchers.IO) {
        AppLogger.i(TAG, "Reboot to DSU requested from subsystem")

        if (PrivilegedProvider.isConnected()) {
            val enabled = runCatching {
                PrivilegedProvider.run {
                    setEnable(true, true)
                }
                true
            }.getOrDefault(false)

            if (enabled) {
                executeSystemReboot(context)
                return@withContext true
            }
        }

        val mode = OperationModeUtils.getOperationMode(
            context = context,
            checkShizuku = Shizuku.pingBinder(),
            checkDhizuku = runCatching { Dhizuku.init(context) && Dhizuku.isPermissionGranted() }.getOrDefault(false),
        )

        AppLogger.i(TAG, "Executing reboot strategy for mode: $mode")

        when (mode) {
            OperationMode.SYSTEM_AND_ROOT,
            OperationMode.ROOT -> {
                val res = Shell.cmd("gsi_tool enable -s && (svc power reboot || reboot)").exec()
                res.isSuccess
            }

            OperationMode.SYSTEM -> {
                runCatching {
                    val pm = context.getSystemService(PowerManager::class.java)
                    pm?.reboot("dsu")
                    true
                }.getOrDefault(false)
            }

            OperationMode.SHIZUKU -> {
                // Shizuku shell pre-11 newProcess API is not public in 13.1.5;
                // Shizuku mode operates through the bound PrivilegedService (handled above).
                // If the service is not connected here, fall through to in-app flow.
                AppLogger.w(TAG, "Shizuku reboot requires bound privileged service; delegating to app")
                false
            }

            OperationMode.DHIZUKU -> {
                // Dhizuku-API has no Dhizuku.reboot(); DeviceOwner reboots via DevicePolicyManager.
                runCatching {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)
                    val admin = Dhizuku.getOwnerComponent()
                    if (dpm != null && admin != null) {
                        dpm.reboot(admin)
                        true
                    } else {
                        val pm = context.getSystemService(PowerManager::class.java)
                        pm?.reboot("dsu")
                        true
                    }
                }.getOrDefault(false)
            }

            OperationMode.ADB -> {
                AppLogger.w(TAG, "ADB unrooted mode cannot trigger reboot autonomously")
                false
            }
        }
    }

    private fun executeSystemReboot(context: Context) {
        runCatching {
            val pm = context.getSystemService(PowerManager::class.java)
            pm?.reboot("dsu")
        }.onFailure {
            Shell.cmd("svc power reboot || reboot").exec()
        }
    }
}
