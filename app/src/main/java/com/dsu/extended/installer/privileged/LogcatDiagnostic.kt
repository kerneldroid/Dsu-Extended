package com.dsu.extended.installer.privileged

import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import com.dsu.extended.model.DiagnosticReport
import com.dsu.extended.model.ErrorAnalysis
import com.dsu.extended.model.ErrorSeverity
import com.dsu.extended.model.GsiInfo
import com.dsu.extended.model.InstallationInfo
import com.dsu.extended.model.LogEntry
import com.dsu.extended.model.LogSeverity
import com.dsu.extended.preparation.InstallationStep
import com.dsu.extended.util.CmdRunner
import com.dsu.extended.util.GsiLogFilter

class LogcatDiagnostic(
    private val onInstallationError: (error: InstallationStep, errorInfo: String) -> Unit,
    private val onStepUpdate: (step: InstallationStep) -> Unit,
    private val onInstallationProgressUpdate: (progress: Float, partition: String) -> Unit,
    private val onInstallationSuccess: () -> Unit,
    private val onLogLineReceived: () -> Unit,
    private val onDiagnosticReportGenerated: ((DiagnosticReport) -> Unit)? = null,
) {

    private val tag = this.javaClass.simpleName
    private val logChunks = ArrayDeque<String>()
    val logs: String get() = logChunks.joinToString(separator = "")
    val isLogging = AtomicBoolean(false)
    var shouldLogEverything = false

    // Enhanced logging features
    private val logEntries = mutableListOf<LogEntry>()
    private var currentGsiInfo: GsiInfo? = null
    private var installationStartTime: Long = 0
    private var lastErrorAnalysis: ErrorAnalysis? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private companion object {
        const val MAX_RETAINED_LOG_CHUNKS = 2000
        const val MAX_RETAINED_LOG_ENTRIES = 5000
    }

    fun setGsiInfo(gsiInfo: GsiInfo) {
        currentGsiInfo = gsiInfo
    }

    fun startLogging(prependString: String) {
        if (isLogging.get()) {
            destroy()
        }
        logChunks.clear()
        logEntries.clear()
        isLogging.set(true)
        installationStartTime = System.currentTimeMillis()

        Log.d(tag, "startLogging(), logEverything: $shouldLogEverything, isLogging: ${isLogging.get()}")
        CmdRunner.run("logcat -b main,system,crash -c")

        val logCmd =
            if (shouldLogEverything) {
                "logcat -b all"
            } else {
                "logcat -b main,system,crash -v tag " +
                    "gsid:V *:S " +
                    "DynamicSystemService:V *:S " +
                    "DynamicSystemInstallationService:V *:S " +
                    "DynSystemInstallationService:V *:S " +
                    "init:V *:S vold:V *:S"
            }

        CmdRunner.runReadEachLine(logCmd) { line ->
            if (!shouldLogEverything && !GsiLogFilter.shouldInclude(line)) {
                return@runReadEachLine
            }

            if (logChunks.isEmpty()) {
                logChunks.clear()
                logChunks.addLast(buildEnhancedHeader(prependString))
            }

            if (!isLogging.get()) {
                return@runReadEachLine
            }

            // Parse and store structured log entry
            val logEntry = parseLogLine(line)
            logEntries.add(logEntry)

            // Format with timestamp and severity
            val formattedLine = formatLogLine(logEntry)
            logChunks.addLast("$formattedLine\n")
            while (logChunks.size > MAX_RETAINED_LOG_CHUNKS) {
                logChunks.removeFirst()
            }
            if (logEntries.size > MAX_RETAINED_LOG_ENTRIES) {
                logEntries.subList(0, logEntries.size - MAX_RETAINED_LOG_ENTRIES).clear()
            }
            onLogLineReceived()

            // Check for installation start
            if (line.contains("DynamicSystemService") && line.contains("startInstallation")) {
                onStepUpdate(InstallationStep.INSTALLING)
                onInstallationProgressUpdate(0F, "userdata")
                addInfoLog("安装已开始", "DynamicSystemService")
            }

            // Enhanced error detection with detailed diagnostics
            processLogLine(line, logEntry)
        }
    }

    private fun buildEnhancedHeader(prependString: String): String {
        return buildString {
            appendLine("╔═══════════════════════════════════════════════════════════════╗")
            appendLine("║       DSU EXTENDED GSI 安装日志 v4.1                          ║")
            appendLine("╚═══════════════════════════════════════════════════════════════╝")
            appendLine()
            appendLine("📅 开始时间：${dateFormat.format(Date(installationStartTime))}")
            appendLine("过滤规则：${if (shouldLogEverything) "无过滤 logcat" else "仅 GSI/DSU 相关标签"}")
            appendLine()
            appendLine(prependString)
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine(if (shouldLogEverything) "📋 实时 LOGCAT（无过滤）" else "📋 实时 GSI 日志")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
        }
    }

    private fun parseLogLine(line: String): LogEntry {
        val logcatPattern = Regex("^([VDIWEF])/([^:]+):\\s*(.+)$")
        val match = logcatPattern.find(line)

        return if (match != null) {
            val (severityStr, logTag, message) = match.destructured
            val severity = when (severityStr) {
                "V" -> LogSeverity.VERBOSE
                "D" -> LogSeverity.DEBUG
                "I" -> LogSeverity.INFO
                "W" -> LogSeverity.WARNING
                "E" -> LogSeverity.ERROR
                "F" -> LogSeverity.FATAL
                else -> LogSeverity.INFO
            }
            LogEntry(
                timestamp = System.currentTimeMillis(),
                severity = severity,
                tag = logTag.trim(),
                message = message,
                source = "logcat",
            )
        } else {
            LogEntry(
                timestamp = System.currentTimeMillis(),
                severity = LogSeverity.INFO,
                tag = "raw",
                message = line,
                source = "raw",
            )
        }
    }

    private fun formatLogLine(entry: LogEntry): String {
        val time = dateFormat.format(Date(entry.timestamp))
        return "${entry.severity.emoji} [$time] ${entry.tag}: ${entry.message}"
    }

    private fun addInfoLog(message: String, source: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            severity = LogSeverity.INFO,
            tag = "DsuExtended",
            message = message,
            source = source,
        )
        logEntries.add(entry)
        logChunks.addLast("${formatLogLine(entry)}\n")
    }

    private fun processLogLine(line: String, logEntry: LogEntry) {
        // Already running DSU check
        if (line.contains("We are already running in DynamicSystem")) {
            handleError(
                InstallationStep.ERROR_ALREADY_RUNNING_DYN_OS,
                "正在运行动态系统时无法安装 DSU",
                ErrorSeverity.ERROR,
            )
            return
        }

        // External SD card allocation error (SELinux denial)
        if (line.contains("realpath failed") && line.contains("Permission denied")) {
            handleError(
                InstallationStep.ERROR_EXTERNAL_SDCARD_ALLOC,
                line,
                ErrorSeverity.ERROR,
                listOf(
                    "SELinux 正在阻止访问 SD 卡",
                    "gsid 无法在外部存储上分配空间",
                ),
                listOf(
                    "请启用「卸载 SD 卡」选项后重试",
                    "安装带有 sepolicy 修复的 Magisk 模块",
                ),
            )
            return
        }

        // Storage threshold error
        if (line.contains("is below the minimum threshold of")) {
            handleError(
                InstallationStep.ERROR_NO_AVAIL_STORAGE,
                line,
                ErrorSeverity.ERROR,
                listOf("可用存储空间不足，无法为 DSU 分配"),
                listOf(
                    "请释放设备存储空间",
                    "删除不常用的应用和文件",
                ),
            )
            return
        }

        // F2FS path error
        if (line.contains("read failed") &&
            line.contains("No such file or directory") &&
            line.contains("f2fs")
        ) {
            handleError(
                InstallationStep.ERROR_F2FS_WRONG_PATH,
                line,
                ErrorSeverity.ERROR,
                listOf("内核将 f2fs 注册为 f2fs_dev 而不是 f2fs"),
                listOf(
                    if (Build.VERSION.SDK_INT >= 35) {
                        "使用包含 A15/A16 f2fs_dev 回退补丁的自定义 gsid 构建"
                    } else {
                        "安装包含自定义 gsid 二进制的 Magisk 模块"
                    },
                    "使用能正确注册 f2fs 的内核",
                ),
            )
            return
        }

        // SELinux block device error
        if (line.contains("Failed to get stat for block device") &&
            line.contains("Permission denied")
        ) {
            handleError(
                InstallationStep.ERROR_SELINUX,
                line,
                ErrorSeverity.ERROR,
                listOf("SELinux 拒绝访问块设备"),
                listOf(
                    "执行：setenforce 0（需要 Root）",
                    "安装带有 sepolicy 修复的 Magisk 模块",
                ),
            )
            return
        }

        // Extents fragmentation error (Android 10)
        if (line.contains("File is too fragmented") && line.contains("512")) {
            handleError(
                InstallationStep.ERROR_EXTENTS,
                line,
                ErrorSeverity.ERROR,
                listOf("存储过于碎片化，无法分配"),
                listOf(
                    "安装提高 extent 上限的 Magisk 模块",
                    "请释放空间并重启后重试",
                ),
            )
            return
        }

        // Installation cancelled
        if (line.contains("NOT_STARTED")) {
            if (line.contains("INSTALL_CANCELLED")) {
                handleError(InstallationStep.ERROR_CANCELED, line, ErrorSeverity.WARNING)
            } else {
                handleError(InstallationStep.ERROR, line, ErrorSeverity.ERROR)
            }
            return
        }

        // NEW: AVB verification errors
        if (line.contains("avb_slot_verify") ||
            line.contains("dm-verity") ||
            (line.contains("Verification") && line.contains("failed"))
        ) {
            handleError(
                InstallationStep.ERROR_AVB_VERIFICATION,
                line,
                ErrorSeverity.CRITICAL,
                listOf(
                    "AVB（Android Verified Boot）校验失败",
                    "dm-verity 正在阻止 GSI 启动",
                ),
                listOf(
                    "刷入已禁用校验的 vbmeta 镜像",
                    "执行：fastboot flash vbmeta vbmeta_disabled.img --disable-verity --disable-verification",
                    "参见：https://developer.android.com/topic/generic-system-image#flash-gsi",
                ),
            )
            return
        }

        // NEW: Kernel panic detection
        if (line.contains("kernel panic", ignoreCase = true) ||
            line.contains("Unable to handle kernel", ignoreCase = true)
        ) {
            handleError(
                InstallationStep.ERROR_KERNEL_MISMATCH,
                line,
                ErrorSeverity.CRITICAL,
                listOf(
                    "检测到内核崩溃——GSI 可能不兼容",
                    "GSI 与设备的内核版本不匹配",
                ),
                listOf(
                    "请尝试为您的内核版本构建的其他 GSI",
                    "请检查 GSI 与您设备的兼容性",
                ),
            )
            return
        }

        // NEW: Vendor mismatch
        if (line.contains("vendor") &&
            (line.contains("mismatch") || line.contains("incompatible") || line.contains("failed"))
        ) {
            handleError(
                InstallationStep.ERROR_VENDOR_MISMATCH,
                line,
                ErrorSeverity.ERROR,
                listOf("vendor 镜像与此 GSI 不兼容"),
                listOf(
                    "请确保 GSI 与您的 vendor VNDK 版本匹配",
                    "请尝试为您的 Android 版本构建的 GSI",
                ),
            )
            return
        }

        // NEW: Super partition errors
        if (line.contains("super") &&
            (line.contains("failed") || line.contains("error") || line.contains("cannot"))
        ) {
            handleError(
                InstallationStep.ERROR_SUPER_PARTITION,
                line,
                ErrorSeverity.ERROR,
                listOf("Super 分区操作失败"),
                listOf(
                    "设备可能无法正确支持动态分区",
                    "请在可用空间更多时重试",
                ),
            )
            return
        }

        // NEW: System server crash
        if (line.contains("system_server") &&
            (line.contains("crash") || line.contains("died") || line.contains("killed"))
        ) {
            handleError(
                InstallationStep.ERROR_SYSTEM_SERVER_CRASH,
                line,
                ErrorSeverity.ERROR,
                listOf("系统服务在启动过程中崩溃"),
                listOf(
                    "该 GSI 可能与您的设备存在兼容性问题",
                    "请尝试更稳定的 GSI 构建",
                ),
            )
            return
        }

        // NEW: VNDK mismatch
        if (line.contains("VNDK") &&
            (line.contains("mismatch") || line.contains("version") || line.contains("incompatible"))
        ) {
            handleError(
                InstallationStep.ERROR_VNDK_MISMATCH,
                line,
                ErrorSeverity.ERROR,
                listOf("检测到 VNDK 版本不匹配"),
                listOf(
                    "检查设备的 VNDK 版本：getprop ro.vndk.version",
                    "请使用与您 VNDK 版本匹配的 GSI",
                ),
            )
            return
        }

        // Progress tracking
        if (line.contains("IN_PROGRESS")) {
            if (line.contains("progress:") && line.contains("partition name:")) {
                try {
                    val progressRgx = "(progress: )([\\d+/]+)".toRegex()
                    val partitionRgx = "(partition name: ([a-z+_]+))".toRegex()

                    val progressText = progressRgx.find(line)!!.groupValues[2].split("/")
                    val progress = (progressText[0].toFloat() / progressText[1].toFloat())

                    val partitionText = partitionRgx.find(line)!!.groupValues[2]

                    onInstallationProgressUpdate(progress, partitionText)
                } catch (_: Exception) {
                    onStepUpdate(InstallationStep.PROCESSING_LOG_READABLE)
                }
            } else {
                onStepUpdate(InstallationStep.PROCESSING_LOG_READABLE)
            }
        }

        // Success detection
        if (line.contains("READY") && line.contains("INSTALL_COMPLETED")) {
            addInfoLog("安装成功完成！", "DynamicSystemService")
            generateFinalDiagnosticReport(true)
            onInstallationSuccess()
            destroy()
            return
        }

        // Android 10 specific handling
        if (line.contains("ACTION_CANCEL_INSTALL")) {
            handleError(InstallationStep.ERROR_CANCELED, line, ErrorSeverity.WARNING)
            return
        }

        if (line.contains("postStatus(): statusCode=2")) {
            onStepUpdate(InstallationStep.PROCESSING_LOG_READABLE)
        }

        if (line.contains("postStatus(): statusCode=3")) {
            addInfoLog("安装成功完成！", "DynSystemInstallationService")
            generateFinalDiagnosticReport(true)
            onInstallationSuccess()
            destroy()
            return
        }

        if (line.contains("postStatus(): statusCode=1")) {
            handleError(InstallationStep.ERROR, line, ErrorSeverity.ERROR)
            return
        }
    }

    private fun handleError(
        step: InstallationStep,
        errorInfo: String,
        severity: ErrorSeverity,
        possibleCauses: List<String> = emptyList(),
        suggestions: List<String> = emptyList(),
    ) {
        // Log error with enhanced formatting
        val errorEntry = LogEntry(
            timestamp = System.currentTimeMillis(),
            severity = LogSeverity.ERROR,
            tag = "DsuExtended",
            message = "错误：${step.name}",
            source = "error_handler",
        )
        logEntries.add(errorEntry)
        logChunks.addLast("\n${formatLogLine(errorEntry)}\n")

        if (possibleCauses.isNotEmpty()) {
            logChunks.addLast("┌─ 可能的原因：\n")
            possibleCauses.forEach { cause ->
                logChunks.addLast("│  • $cause\n")
            }
        }

        if (suggestions.isNotEmpty()) {
            logChunks.addLast("├─ 建议：\n")
            suggestions.forEach { suggestion ->
                logChunks.addLast("│  💡 $suggestion\n")
            }
        }

        lastErrorAnalysis = ErrorAnalysis(
            errorType = step.name,
            errorMessage = errorInfo,
            severity = severity,
            possibleCauses = possibleCauses,
        )

        generateFinalDiagnosticReport(false)
        onInstallationError(step, errorInfo)
        destroy()
    }

    private fun generateFinalDiagnosticReport(wasSuccessful: Boolean) {
        val installationInfo = InstallationInfo(
            startTime = installationStartTime,
            endTime = System.currentTimeMillis(),
            wasSuccessful = wasSuccessful,
        )

        val report = DiagnosticReport(
            gsiInfo = currentGsiInfo,
            installationInfo = installationInfo,
            logs = logEntries,
        )

        onDiagnosticReportGenerated?.invoke(report)

        // Append diagnostic summary to logs
        logChunks.addLast("\n")
        logChunks.addLast("═══════════════════════════════════════════════════════════════\n")
        logChunks.addLast("📊 诊断摘要\n")
        logChunks.addLast("═══════════════════════════════════════════════════════════════\n")
        logChunks.addLast("状态：${if (wasSuccessful) "✅ 成功" else "❌ 失败"}\n")
        logChunks.addLast("耗时：${(System.currentTimeMillis() - installationStartTime) / 1000}s\n")

        if (!wasSuccessful && report.errorAnalysis != null) {
            logChunks.addLast("错误类型：${report.errorAnalysis.errorType}\n")
            logChunks.addLast("严重程度：${report.errorAnalysis.severity.emoji} ${report.errorAnalysis.severity.name}\n")
        }

        if (report.suggestions.isNotEmpty()) {
            logChunks.addLast("\n💡 建议：\n")
            report.suggestions.forEachIndexed { index, suggestion ->
                logChunks.addLast("${index + 1}. $suggestion\n")
            }
        }
    }

    fun destroy() {
        CmdRunner.destroy()
        isLogging.set(false)
        Log.d(tag, "destroy(), isLogging: ${isLogging.get()}")
    }
}
