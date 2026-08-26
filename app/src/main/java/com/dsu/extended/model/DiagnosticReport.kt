package com.dsu.extended.model

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Comprehensive diagnostic report for DSU installation issues
 */
data class DiagnosticReport(
    val timestamp: Long = System.currentTimeMillis(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val gsiInfo: GsiInfo? = null,
    val installationInfo: InstallationInfo? = null,
    val errorAnalysis: ErrorAnalysis? = null,
    val logs: List<LogEntry> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val device: String = Build.DEVICE,
    val board: String = Build.BOARD,
    val hardware: String = Build.HARDWARE,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val securityPatch: String = Build.VERSION.SECURITY_PATCH,
    val fingerprint: String = Build.FINGERPRINT,
    val cpuAbi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
    val allAbis: List<String> = Build.SUPPORTED_ABIS.toList(),
    val bootloader: String = Build.BOOTLOADER,
    val isEmulator: Boolean = Build.FINGERPRINT.contains("generic") ||
        Build.MODEL.contains("Emulator") ||
        Build.BRAND.contains("generic"),
) {
}

data class GsiInfo(
    val fileName: String = "",
    val fileSize: Long = 0,
    val fileHash: String = "",
    val expectedArch: String = "",
    val expectedVndk: String = "",
    val isGoogleGsi: Boolean = false,
    val isTrebleRequired: Boolean = true,
)

data class InstallationInfo(
    val operationMode: String = "",
    val userdataSize: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val wasSuccessful: Boolean = false,
    val partitionsInstalled: List<String> = emptyList(),
)

data class ErrorAnalysis(
    val errorType: String = "",
    val errorCode: Int = 0,
    val errorMessage: String = "",
    val errorSource: String = "",
    val stackTrace: String? = null,
    val relatedLogLines: List<String> = emptyList(),
    val severity: ErrorSeverity = ErrorSeverity.UNKNOWN,
    val isRecoverable: Boolean = false,
    val possibleCauses: List<String> = emptyList(),
)

enum class ErrorSeverity(val level: Int, val emoji: String) {
    DEBUG(0, "🔍"),
    INFO(1, "ℹ️"),
    WARNING(2, "⚠️"),
    ERROR(3, "❌"),
    CRITICAL(4, "🚨"),
    UNKNOWN(-1, "❓"),
}

enum class LogSeverity(val tag: String, val emoji: String) {
    VERBOSE("V", "📝"),
    DEBUG("D", "🔍"),
    INFO("I", "ℹ️"),
    WARNING("W", "⚠️"),
    ERROR("E", "❌"),
    FATAL("F", "💀"),
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val severity: LogSeverity = LogSeverity.INFO,
    val tag: String = "",
    val message: String = "",
    val source: String = "",
) {
    fun toFormattedString(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val time = dateFormat.format(Date(timestamp))
        return "${severity.emoji} [$time] $tag: $message"
    }
}

/**
 * Known error patterns for diagnosis
 */
object ErrorPatterns {
    // AVB related
    val AVB_PATTERNS = listOf(
        "avb_slot_verify" to "AVB slot verification failed",
        "dm-verity" to "dm-verity verification error",
        "vbmeta" to "vbmeta image verification failed",
        "avb: Error" to "AVB error detected",
        "Verification failed" to "Image verification failed",
        "LOCKED" to "Device may have locked bootloader",
    )

    // Boot failures
    val BOOT_PATTERNS = listOf(
        "bootloop" to "System is in bootloop",
        "cannot find" to "Required file or partition not found",
        "kernel panic" to "Kernel panic occurred",
        "init: " to "Init process error",
        "Rebooting in" to "System crash leading to reboot",
        "watchdog" to "Watchdog timeout detected",
    )

    // Partition issues
    val PARTITION_PATTERNS = listOf(
        "super" to "Super partition error",
        "slot_suffix" to "Slot switching error",
        "mount failed" to "Partition mount failed",
        "resize" to "Partition resize error",
        "dynamic_partitions" to "Dynamic partition error",
    )

    // Compatibility issues
    val COMPATIBILITY_PATTERNS = listOf(
        "VNDK" to "VNDK version mismatch",
        "vendor" to "Vendor compatibility issue",
        "treble" to "Treble compliance issue",
        "hal" to "HAL compatibility issue",
        "selinux" to "SELinux policy issue",
    )

    // System errors
    val SYSTEM_PATTERNS = listOf(
        "system_server" to "System server crash",
        "zygote" to "Zygote process error",
        "ServiceManager" to "Service manager error",
        "PackageManager" to "Package manager error",
        "ActivityManager" to "Activity manager error",
    )
}
