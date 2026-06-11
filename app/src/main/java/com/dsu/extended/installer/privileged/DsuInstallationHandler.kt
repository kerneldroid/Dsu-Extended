package com.dsu.extended.installer.privileged

import android.content.Intent
import android.os.storage.VolumeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.dsu.extended.model.Session
import com.dsu.extended.service.PrivilegedProvider
import com.dsu.extended.util.AppLogger

/**
 * Install images via DSU app
 * Supported modes are: Shizuku (as shell or root), root and system
 */
open class DsuInstallationHandler(
    private val session: Session,
) {

    private val tag = this.javaClass.simpleName

    fun startInstallation() {
        AppLogger.i(
            tag,
            "Starting privileged installation flow",
            "unmountSd" to session.preferences.isUnmountSdCard,
            "uri" to session.dsuInstallation.uri,
            "size" to session.dsuInstallation.fileSize,
        )
        if (session.preferences.isUnmountSdCard) {
            unmountSdTemporary()
        }
        forwardInstallationToDSU()
    }

    private fun forwardInstallationToDSU() {
        val userdataSize = session.userSelection.userSelectedUserdata
        val fileUri = session.dsuInstallation.uri
        val length = session.dsuInstallation.fileSize

        PrivilegedProvider.run {
            setDynProp()
            forceStopPackage("com.android.dynsystem")

            val dynIntent = Intent()
            dynIntent.setClassName(
                "com.android.dynsystem",
                "com.android.dynsystem.VerificationActivity",
            )
            dynIntent.flags += Intent.FLAG_ACTIVITY_NEW_TASK
            dynIntent.action = "android.os.image.action.START_INSTALL"
            dynIntent.data = fileUri
            dynIntent.putExtra("KEY_USERDATA_SIZE", userdataSize)
            dynIntent.putExtra("KEY_SYSTEM_SIZE", length)

            AppLogger.i(tag, "Launching DSU VerificationActivity", "intent" to dynIntent)
            startActivity(dynIntent)
        }
    }

    private fun unmountSdTemporary() {
        val volumes: List<VolumeInfo> =
            PrivilegedProvider.getService().volumes
        val volumesUnmount: ArrayList<String> = ArrayList()
        for (volume in volumes) {
            val volumeId = volume.id ?: continue
            if (volumeId.contains("public")) {
                PrivilegedProvider.run { unmount(volumeId) }
                volumesUnmount.add(volumeId)
                AppLogger.i(tag, "Volume unmounted", "id" to volumeId)
            }
        }
        if (volumesUnmount.size > 0) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                delay(30 * 1000)
                for (volume in volumesUnmount) {
                    AppLogger.i(tag, "Volume remounted", "id" to volume)
                    PrivilegedProvider.run { mount(volume) }
                }
            }
        }
    }
}
