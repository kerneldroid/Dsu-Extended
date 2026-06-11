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

class LogcatDiagnostic(
    private val onInstallationError: (error: InstallationStep, errorInfo: String) -> Unit,
    private val onStepUpdate: (step: InstallationStep) -> Unit,
    private val onInstallationProgressUpdate: (progress: Float, partition: String) -> Unit,
    private val onInstallationSuccess: () -> Unit,
    private val onLogLineReceived: () -> Unit,
    private val onDiagnosticReportGenerated: ((DiagnosticReport) -> Unit)? = null,
) {

    private val tag = this.javaClass.simpleName
    var logs = ""
    val isLogging = AtomicBoolean(false)
    var shouldLogEverything = false

    // Enhanced logging features
    private val logEntries = mutableListOf<LogEntry>()
    private var currentGsiInfo: GsiInfo? = null
    private var installationStartTime: Long = 0
    private var lastErrorAnalysis: ErrorAnalysis? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun setGsiInfo(gsiInfo: GsiInfo) {
        currentGsiInfo = gsiInfo
    }

    fun startLogging(prependString: String) {
        if (isLogging.get()) {
            destroy()
        }
        logs = ""
        logEntries.clear()
        isLogging.set(true)
        installationStartTime = System.currentTimeMillis()

        Log.d(tag, "startLogging(), logEverything: $shouldLogEverything, isLogging: ${isLogging.get()}")
        CmdRunner.run("logcat -c")

        val logCmd =
            if (shouldLogEverything) {
                "logcat"
            } else {
                "logcat -v tag gsid:* *:S DynamicSystemService:* *:S DynamicSystemInstallationService:* *:S DynSystemInstallationService:* *:S init:* *:S vold:* *:S"
            }

        CmdRunner.runReadEachLine(logCmd) { line ->
            if (logs.isEmpty()) {
                logs = buildEnhancedHeader(prependString)
            }

            if (!isLogging.get()) {
                return@runReadEachLine
            }

            // Parse and store structured log entry
            val logEntry = parseLogLine(line)
            logEntries.add(logEntry)

            // Format with timestamp and severity
            val formattedLine = formatLogLine(logEntry)
            logs += "$formattedLine\n"
            onLogLineReceived()

            // Check for installation start
            if (line.contains("DynamicSystemService") && line.contains("startInstallation")) {
                onStepUpdate(InstallationStep.INSTALLING)
                onInstallationProgressUpdate(0F, "userdata")
                addInfoLog("Installation started", "DynamicSystemService")
            }

            // Enhanced error detection with detailed diagnostics
            processLogLine(line, logEntry)
        }
    }

    private fun buildEnhancedHeader(prependString: String): String {
        return buildString {
            appendLine("╔═══════════════════════════════════════════════════════════════╗")
            appendLine("║          DSU EXTENDED INSTALLATION LOG v4.0                   ║")
            appendLine("╚═══════════════════════════════════════════════════════════════╝")
            appendLine()
            appendLine("📅 Started: ${dateFormat.format(Date(installationStartTime))}")
            appendLine()
            appendLine(prependString)
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("📋 LIVE LOGS")
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
        logs += "${formatLogLine(entry)}\n"
    }

    private fun processLogLine(line: String, logEntry: LogEntry) {
        // Already running DSU check
        if (line.contains("We are already running in DynamicSystem")) {
            handleError(
                InstallationStep.ERROR_ALREADY_RUNNING_DYN_OS,
                "Cannot install DSU while running a Dynamic System",
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
                    "SELinux is blocking SD card access",
                    "gsid cannot allocate to external storage",
                ),
                listOf(
                    "Enable 'Unmount SD' option and retry",
                    "Install Magisk module with sepolicy fixes",
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
                listOf("Insufficient free storage for DSU allocation"),
                listOf(
                    "Free up storage space on device",
                    "Delete unused apps and files",
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
                listOf("Kernel registers f2fs as f2fs_dev instead of f2fs"),
                listOf(
                    if (Build.VERSION.SDK_INT >= 35) {
                        "Use a custom gsid build that includes the A15/A16 f2fs_dev fallback patch"
                    } else {
                        "Install Magisk module with custom gsid binary"
                    },
                    "Use a kernel with correct f2fs registration",
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
                listOf("SELinux denying access to block device"),
                listOf(
                    "Run: setenforce 0 (requires root)",
                    "Install Magisk module with sepolicy fixes",
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
                listOf("Storage too fragmented for allocation"),
                listOf(
                    "Install Magisk module with higher extent limit",
                    "Try after freeing storage and rebooting",
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
                    "AVB (Android Verified Boot) verification failed",
                    "dm-verity is blocking the GSI boot",
                ),
                listOf(
                    "Flash disabled vbmeta image",
                    "Run: fastboot flash vbmeta vbmeta_disabled.img --disable-verity --disable-verification",
                    "See: https://developer.android.com/topic/generic-system-image#flash-gsi",
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
                    "Kernel panic detected - GSI may be incompatible",
                    "Kernel version mismatch between GSI and device",
                ),
                listOf(
                    "Try a different GSI built for your kernel version",
                    "Check GSI compatibility with your device",
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
                listOf("Vendor image incompatible with this GSI"),
                listOf(
                    "Ensure GSI matches your vendor VNDK version",
                    "Try a GSI built for your Android version",
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
                listOf("Super partition operation failed"),
                listOf(
                    "Device may not properly support Dynamic Partitions",
                    "Try with more free storage space",
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
                listOf("System server crashed during boot"),
                listOf(
                    "GSI may have compatibility issues with your device",
                    "Try a more stable GSI build",
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
                listOf("VNDK version mismatch detected"),
                listOf(
                    "Check your device's VNDK version: getprop ro.vndk.version",
                    "Use a GSI matching your VNDK version",
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
            addInfoLog("Installation completed successfully!", "DynamicSystemService")
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
            addInfoLog("Installation completed successfully!", "DynSystemInstallationService")
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
            message = "ERROR: ${step.name}",
            source = "error_handler",
        )
        logEntries.add(errorEntry)
        logs += "\n${formatLogLine(errorEntry)}\n"

        if (possibleCauses.isNotEmpty()) {
            logs += "┌─ Possible causes:\n"
            possibleCauses.forEach { cause ->
                logs += "│  • $cause\n"
            }
        }

        if (suggestions.isNotEmpty()) {
            logs += "├─ Suggestions:\n"
            suggestions.forEach { suggestion ->
                logs += "│  💡 $suggestion\n"
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

        val report = if (wasSuccessful) {
            DiagnosticReport(
                gsiInfo = currentGsiInfo,
                installationInfo = installationInfo,
                logs = logEntries,
            )
        } else {
            DiagnosticReport(
                gsiInfo = currentGsiInfo,
                installationInfo = installationInfo,
                logs = logEntries,
            )
        }

        onDiagnosticReportGenerated?.invoke(report)

        // Append diagnostic summary to logs
        logs += "\n"
        logs += "═══════════════════════════════════════════════════════════════\n"
        logs += "📊 DIAGNOSTIC SUMMARY\n"
        logs += "═══════════════════════════════════════════════════════════════\n"
        logs += "Status: ${if (wasSuccessful) "✅ SUCCESS" else "❌ FAILED"}\n"
        logs += "Duration: ${(System.currentTimeMillis() - installationStartTime) / 1000}s\n"

        if (!wasSuccessful && report.errorAnalysis != null) {
            logs += "Error Type: ${report.errorAnalysis.errorType}\n"
            logs += "Severity: ${report.errorAnalysis.severity.emoji} ${report.errorAnalysis.severity.name}\n"
        }

        if (report.suggestions.isNotEmpty()) {
            logs += "\n💡 SUGGESTIONS:\n"
            report.suggestions.forEachIndexed { index, suggestion ->
                logs += "${index + 1}. $suggestion\n"
            }
        }
    }

    fun destroy() {
        CmdRunner.destroy()
        isLogging.set(false)
        Log.d(tag, "destroy(), isLogging: ${isLogging.get()}")
    }
}
