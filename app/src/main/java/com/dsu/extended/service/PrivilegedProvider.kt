package com.dsu.extended.service

import com.dsu.extended.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.dsu.extended.IPrivilegedService

object PrivilegedProvider {

    private val tag = this.javaClass.simpleName

    var connection = Connection()

    suspend fun run(
        onFail: () -> Unit = {},
        onConnected: suspend IPrivilegedService.() -> Unit,
    ) {
        if (isConnected()) {
            val initialService = connection.SERVICE
            if (initialService != null) {
                onConnected(initialService)
                return
            }
        }
        var timeout = 0
        while (!isConnected()) {
            timeout += 1000
            if (timeout > 20000) {
                AppLogger.e(tag, "Privileged service unavailable", null, "timeoutMs" to timeout)
                onFail()
                return
            }
            delay(1000)
            AppLogger.d(tag, "Waiting privileged service", "elapsedSeconds" to (timeout / 1000))
        }

        val connectedService = connection.SERVICE
        if (connectedService == null) {
            AppLogger.e(tag, "Privileged service disconnected before use")
            onFail()
            return
        }
        AppLogger.i(tag, "Privileged service connected", "uid" to connectedService.uid)
        onConnected(connectedService)
    }

    // Blocking
    fun getService(): IPrivilegedService {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            AppLogger.e(tag, "getService() called from main thread! This will cause ANR.")
            // We still continue to not break existing logic, but this must be fixed
        }
        var timeout = 0
        while (!isConnected()) {
            timeout += 500
            if (timeout > 20000) {
                throw IllegalStateException("Privileged service unavailable after ${timeout / 1000}s")
            }
            Thread.sleep(500)
        }
        return this.connection.SERVICE
            ?: throw IllegalStateException("Privileged service became unavailable")
    }

    // Blocking
    fun isRoot(): Boolean {
        return this.getService().uid == 0
    }

    fun isConnected(): Boolean {
        return this.connection.SERVICE != null
    }
}
