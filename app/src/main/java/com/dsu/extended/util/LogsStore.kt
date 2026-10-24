package com.dsu.extended.util

import android.content.Context
import com.topjohnwu.superuser.Shell
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class StoredLogType(val slug: String) {
    INSTALLATION("installation"),
    AUTO_MODE("auto_mode"),
    GSI("gsi"),
    CAT("cat"),
    APP("app"),
    BOOT("boot"),
    OTHER("other");

    companion object {
        fun fromSlug(slug: String): StoredLogType {
            return entries.firstOrNull { it.slug == slug } ?: OTHER
        }
    }
}

data class StoredLogEntry(
    val filePath: String,
    val title: String,
    val type: StoredLogType,
    val createdAtMillis: Long,
    val sizeBytes: Long,
)

object LogsStore {
    private const val LOGS_DIR_NAME = "dsu_logs"
    private val filePattern = Regex("^([a-z_]+)_([0-9]{8}_[0-9]{6})_(.+)\\.log$")
    private val timestampFileFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US) }

    private fun logsDir(context: Context): File {
        val dir = File(context.filesDir, LOGS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun sanitizeTitle(raw: String): String {
        val compact = raw.trim().replace("\\s+".toRegex(), "_")
        val safe = compact.replace("[\\\\/:*?\"<>|]".toRegex(), "")
        return safe.take(64).ifBlank { "log" }
    }

    fun writeLog(
        context: Context,
        type: StoredLogType,
        title: String,
        content: String,
    ): StoredLogEntry {
        val timestamp = timestampFileFormat.get().format(Date())
        val normalizedTitle = sanitizeTitle(title)
        val file = File(logsDir(context), "${type.slug}_${timestamp}_$normalizedTitle.log")
        file.writeText(content, Charsets.UTF_8)
        return parseFile(file)
    }

    fun readLog(filePath: String): String {
        return runCatching {
            File(filePath).readText(Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun deleteLog(filePath: String): Boolean {
        return runCatching { File(filePath).delete() }.getOrDefault(false)
    }

    fun renameLog(filePath: String, newTitle: String): StoredLogEntry? {
        val file = File(filePath)
        if (!file.exists()) {
            return null
        }
        val match = filePattern.matchEntire(file.name)
        val current = parseFile(file)
        val timestamp = match?.groupValues?.getOrNull(2) ?: timestampFileFormat.get().format(Date(current.createdAtMillis))
        val type = match?.groupValues?.getOrNull(1) ?: current.type.slug
        val renamed = File(file.parentFile, "${type}_${timestamp}_${sanitizeTitle(newTitle)}.log")
        if (renamed.absolutePath == file.absolutePath) {
            return current
        }
        val moved = runCatching { file.renameTo(renamed) }.getOrDefault(false)
        return if (moved) parseFile(renamed) else null
    }

    fun listLogs(context: Context): List<StoredLogEntry> {
        val dir = logsDir(context)
        val files = dir.listFiles().orEmpty().filter { it.isFile && it.extension == "log" }
        return files
            .map { parseFile(it) }
            .sortedByDescending { it.createdAtMillis }
    }

    fun captureGsiLogs(context: Context): Result<StoredLogEntry> {
        return runCatching {
            val isRoot = runCatching { Shell.getShell().isRoot }.getOrDefault(false)
            val content = buildGsiSnapshotText(isRoot)
            writeLog(
                context = context,
                type = StoredLogType.GSI,
                title = "gsi_snapshot",
                content = content,
            )
        }
    }

    private fun buildGsiSnapshotText(isRoot: Boolean): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val builder = StringBuilder()
        builder.appendLine("DSU Extended GSI log snapshot")
        builder.appendLine("generatedAt=$now")
        builder.appendLine("filter=GSI/DSU-related lines only")
        builder.appendLine("root=$isRoot")
        builder.appendLine()
        builder.appendLine("=== GSI / DSU properties ===")
        listOf(
            "gsid.image_installed",
            "ro.gsid.image_running",
            "ro.boot.dynamic_partitions",
            "ro.boot.dynamic_partitions_retrofit",
            "ro.treble.enabled",
            "ro.vndk.version",
            "ro.product.cpu.abi",
            "ro.build.version.sdk",
        ).forEach { key ->
            builder.appendLine("$key=${runShell("getprop $key").trim()}")
        }

        builder.appendLine()
        builder.appendLine("=== logcat (filtered GSI/DSU lines) ===")
        val logcat = filteredOrError(
            runShell("logcat -d -b main,system,crash -v threadtime -t $MAX_LOGCAT_LINES"),
        )
        builder.appendLine(logcat.ifBlank { "(no GSI/DSU-related logcat lines)" })

        builder.appendLine()
        builder.appendLine("=== /data/gsi ===")
        if (isRoot) {
            builder.appendLine(runShell("ls -la /data/gsi 2>&1"))
            builder.appendLine(runShell("du -sh /data/gsi/* 2>&1 | tail -n 50"))
        } else {
            builder.appendLine("Root is unavailable; /data/gsi listing skipped.")
        }

        builder.appendLine()
        builder.appendLine("=== dmesg (filtered, tail $MAX_KERNEL_LINES) ===")
        if (isRoot) {
            val dmesg = filteredKernelOrError(
                runShell("dmesg -T 2>/dev/null | tail -n $MAX_KERNEL_LINES"),
            )
            builder.appendLine(dmesg.ifBlank { "(no GSI/DSU-related kernel lines)" })
        } else {
            builder.appendLine("Root is unavailable; dmesg skipped.")
        }

        builder.appendLine()
        builder.appendLine("=== /proc/last_kmsg (filtered) ===")
        if (isRoot) {
            val lastKmsg = filteredKernelOrError(
                runShell("cat /proc/last_kmsg 2>/dev/null | tail -n $MAX_KERNEL_LINES"),
            )
            builder.appendLine(lastKmsg.ifBlank { "(last_kmsg is unavailable or has no GSI/DSU lines)" })
        } else {
            builder.appendLine("Root is unavailable; last_kmsg skipped.")
        }

        builder.appendLine()
        builder.appendLine("=== /sys/fs/pstore (filtered) ===")
        if (isRoot) {
            val pstore = filteredKernelOrError(
                runShell(
                    "if [ -d /sys/fs/pstore ]; then " +
                        "for f in /sys/fs/pstore/*; do " +
                        "[ -f \"${'$'}f\" ] && echo \"--- ${'$'}f ---\" && cat \"${'$'}f\" && echo; " +
                        "done; " +
                        "else echo \"pstore not available\"; fi",
                ),
            )
            builder.appendLine(pstore.ifBlank { "(pstore has no GSI/DSU lines)" })
        } else {
            builder.appendLine("Root is unavailable; pstore skipped.")
        }

        builder.appendLine()
        builder.appendLine("=== app events (buffered) ===")
        builder.appendLine(AppLogger.dumpBufferedLogs().ifBlank { "(empty)" })
        return builder.toString()
    }

    private fun filteredOrError(raw: String): String {
        return if (raw.startsWith("stderr:") || raw.startsWith("command failed:")) {
            raw
        } else {
            GsiLogFilter.filter(raw)
        }
    }

    private fun filteredKernelOrError(raw: String): String {
        return if (raw.startsWith("stderr:") || raw.startsWith("command failed:")) {
            raw
        } else {
            GsiLogFilter.filterKernel(raw)
        }
    }

    private fun runShell(command: String): String {
        return runCatching {
            val result = Shell.cmd(command).exec()
            val out = result.out.joinToString("\n").trim()
            val err = result.err.joinToString("\n").trim()
            if (out.isNotBlank()) {
                out
            } else if (err.isNotBlank()) {
                "stderr:\n$err"
            } else {
                "(empty)"
            }
        }.getOrElse {
            "command failed: ${it.message}"
        }
    }

    private const val MAX_LOGCAT_LINES = 40000
    private const val MAX_KERNEL_LINES = 5000

    private fun parseFile(file: File): StoredLogEntry {
        val match = filePattern.matchEntire(file.name)
        val parsedType = StoredLogType.fromSlug(match?.groupValues?.getOrNull(1).orEmpty())
        val parsedTitle = match?.groupValues?.getOrNull(3)?.replace('_', ' ') ?: file.nameWithoutExtension
        val parsedDate = match?.groupValues?.getOrNull(2)
        val createdAt = runCatching {
            if (parsedDate.isNullOrBlank()) {
                file.lastModified()
            } else {
                timestampFileFormat.get().parse(parsedDate)?.time ?: file.lastModified()
            }
        }.getOrDefault(file.lastModified())
        return StoredLogEntry(
            filePath = file.absolutePath,
            title = parsedTitle,
            type = parsedType,
            createdAtMillis = createdAt,
            sizeBytes = file.length(),
        )
    }
}
