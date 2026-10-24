package com.dsu.extended.compat

import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemProperties
import java.io.File
import com.dsu.extended.util.DevicePropUtils

/**
 * Collects the device-side half of the compatibility decision. Every value is
 * best-effort: an unreadable property becomes null instead of a guessed value.
 */
class DeviceProfileReader {

    fun read(): DeviceProfile {
        val statFs = runCatching {
            StatFs(Environment.getDataDirectory().absolutePath)
        }.getOrNull()
        val totalData = statFs?.let { it.blockCountLong * it.blockSizeLong } ?: 0L
        val freeData = statFs?.let { it.availableBlocksLong * it.blockSizeLong } ?: 0L

        return DeviceProfile(
            sdk = Build.VERSION.SDK_INT,
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            model = Build.MODEL.orEmpty().trim(),
            manufacturer = Build.MANUFACTURER.orEmpty().trim(),
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            trebleEnabled = readBooleanProperty("ro.treble.enabled"),
            crossVersionSupported = readCrossVersionSupport(),
            firstApiLevel = readIntProperty("ro.product.first_api_level")
                ?: readIntProperty("ro.board.first_api_level")
                ?: readIntProperty("ro.vendor.build.version.sdk"),
            vndkVersion = readProperty("ro.vndk.version") ?: readProperty("ro.vndk.lite"),
            dynamicPartitions = readBooleanProperty("ro.boot.dynamic_partitions") == true ||
                readBooleanProperty("ro.boot.dynamic_partitions_retrofit") == true,
            bootloaderLocked = readBootloaderLocked(),
            freeDataBytes = freeData,
            totalDataBytes = totalData,
            gsidMinAllocFraction = DevicePropUtils.getGsidBinaryAllowedPerc(),
            dsuInstalled = DevicePropUtils.isGsiInstalled(),
            dsuRunning = DevicePropUtils.isGsiRunning(),
            securityPatch = Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Official cross-version test from developer.android.com/topic/generic-system-image:
     * the [vendor] section of the version identifier configuration must isolate
     * namespace.default. When the file is absent, null means "unknown".
     */
    private fun readCrossVersionSupport(): Boolean? {
        val file = File(LD_CONFIG_PATH)
        if (!file.isFile || !file.canRead()) return null
        return runCatching {
            var inVendorSection = false
            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.startsWith("[")) {
                        inVendorSection = line == "[vendor]"
                        return@forEach
                    }
                    if (inVendorSection && line.startsWith(NAMESPACE_ISOLATED_KEY)) {
                        val value = line.substringAfter('=', "").trim()
                        if (value.equals("true", ignoreCase = true)) return@useLines true
                        if (value.equals("false", ignoreCase = true)) return@useLines false
                    }
                }
                null
            }
        }.getOrNull()
    }

    private fun readBootloaderLocked(): Boolean? {
        when (readProperty("ro.boot.flash.locked")) {
            "1" -> return true
            "0" -> return false
        }
        return when (readProperty("ro.boot.verifiedbootstate")?.lowercase()) {
            "green" -> true
            "orange", "yellow" -> false
            else -> null
        }
    }

    private fun readBooleanProperty(key: String): Boolean? {
        return readProperty(key)?.toBooleanStrictOrNull()
    }

    private fun readIntProperty(key: String): Int? {
        return readProperty(key)?.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun readProperty(key: String): String? {
        return runCatching { SystemProperties.get(key) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val LD_CONFIG_PATH = "/system/etc/ld.config.version_identifier.txt"
        const val NAMESPACE_ISOLATED_KEY = "namespace.default.isolated"
    }
}
