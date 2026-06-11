package com.dsu.extended.util

import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object CmdRunner {

    private val activeProcesses = Collections.newSetFromMap(ConcurrentHashMap<Process, Boolean>())

    fun run(cmd: String): String {
        return if (Shell.getShell().isRoot) {
            Shell.cmd(cmd).exec().out.joinToString("\n")
        } else {
            runCommandSync(cmd)
        }
    }

    fun runReadEachLine(cmd: String, onReceive: (String) -> Unit) {
        if (Shell.getShell().isRoot) {
            val callbackList: CallbackList<String> = object : CallbackList<String>() {
                override fun onAddElement(s: String) {
                    onReceive(s)
                }
            }
            Shell.cmd(cmd).to(callbackList).submit()
        } else {
            Thread {
                runCommandStreaming(cmd, onReceive)
            }.start()
        }
    }

    private fun runCommandStreaming(cmd: String, onReceive: (String) -> Unit) {
        try {
            val process = ProcessBuilder("/system/bin/sh", "-c", cmd).start()
            activeProcesses.add(process)
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                line?.let { if (it.isNotEmpty()) onReceive(it) }
            }
            process.waitFor()
            activeProcesses.remove(process)
        } catch (e: Exception) {
            AppLogger.e("CmdRunner", "Error running command: $cmd", e)
        }
    }

    private fun runCommandSync(cmd: String): String {
        val output = StringBuilder()
        try {
            val process = ProcessBuilder("/system/bin/sh", "-c", cmd).start()
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                line?.let { output.append(it).append("\n") }
            }
            process.waitFor()
        } catch (e: Exception) {
            AppLogger.e("CmdRunner", "Error running sync command: $cmd", e)
        }
        return output.toString()
    }

    fun destroy() {
        if (Shell.getShell().isRoot) {
            Shell.getShell().close()
        }
        val iterator = activeProcesses.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.destroy()
            iterator.remove()
        }
    }
}
