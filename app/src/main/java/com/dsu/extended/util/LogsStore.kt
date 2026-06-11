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
    private val timestampFileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

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
        val timestamp = timestampFileFormat.format(Date())
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
        val timestamp = match?.groupValues?.getOrNull(2) ?: timestampFileFormat.format(Date(current.createdAtMillis))
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

    fun captureCatExperimental(context: Context): Result<StoredLogEntry> {
        return runCatching {
            val isRoot = runCatching { Shell.getShell().isRoot }.getOrDefault(false)
            val logText = buildCatSnapshotText(isRoot)
            writeLog(
                context = context,
                type = StoredLogType.CAT,
                title = "cat_snapshot",
                content = logText,
            )
        }
    }

    private fun buildCatSnapshotText(isRoot: Boolean): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val builder = StringBuilder()
        builder.appendLine("DSU Extended Cat Snapshot")
        builder.appendLine("generatedAt=$now")
        builder.appendLine("root=$isRoot")
        builder.appendLine()
        builder.appendLine("=== logcat -d -b all -v threadtime (tail 5000) ===")
        builder.appendLine(runShell("logcat -d -b all -v threadtime | tail -n 5000"))
        builder.appendLine()
        builder.appendLine("=== app-events (buffered) ===")
        builder.appendLine(AppLogger.dumpBufferedLogs().ifBlank { "(empty)" })
        if (isRoot) {
            builder.appendLine()
            builder.appendLine("=== dmesg -T (tail 2000) ===")
            builder.appendLine(runShell("dmesg -T | tail -n 2000"))
            builder.appendLine()
            builder.appendLine("=== /proc/last_kmsg ===")
            builder.appendLine(runShell("cat /proc/last_kmsg 2>/dev/null"))
            builder.appendLine()
            builder.appendLine("=== /sys/fs/pstore/* ===")
            builder.appendLine(
                runShell(
                    "if [ -d /sys/fs/pstore ]; then " +
                        "for f in /sys/fs/pstore/*; do " +
                        "[ -f \"${'$'}f\" ] && echo \"--- ${'$'}f ---\" && cat \"${'$'}f\" && echo; " +
                        "done; " +
                        "else echo \"pstore not available\"; fi",
                ),
            )
        } else {
            builder.appendLine()
            builder.appendLine("Root is unavailable. Kernel/pstore sections were skipped.")
        }
        return builder.toString()
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

    private fun parseFile(file: File): StoredLogEntry {
        val match = filePattern.matchEntire(file.name)
        val parsedType = StoredLogType.fromSlug(match?.groupValues?.getOrNull(1).orEmpty())
        val parsedTitle = match?.groupValues?.getOrNull(3)?.replace('_', ' ') ?: file.nameWithoutExtension
        val parsedDate = match?.groupValues?.getOrNull(2)
        val createdAt = runCatching {
            if (parsedDate.isNullOrBlank()) {
                file.lastModified()
            } else {
                timestampFileFormat.parse(parsedDate)?.time ?: file.lastModified()
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
