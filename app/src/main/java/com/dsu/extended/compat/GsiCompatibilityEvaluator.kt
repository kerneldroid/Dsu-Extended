package com.dsu.extended.compat

import java.util.Locale
import kotlin.math.max

/**
 * Turns [GsiFacts] plus [DeviceProfile] into a transparent compatibility report.
 *
 * The rules follow the public GSI requirements: Treble support, a matching CPU
 * ABI, a system image that DSU can install, and the VNDK cross-version support
 * reported by the device. Anything the app cannot prove is downgraded to a
 * warning instead of being presented as a fact.
 */
class GsiCompatibilityEvaluator {

    fun evaluate(
        facts: GsiFacts,
        device: DeviceProfile,
        userdataBytes: Long,
        imageLabel: String = "",
    ): GsiCompatibilityReport {
        val findings = mutableListOf<CompatibilityFinding>()
        val rawSize = facts.effectiveRawSize
        val requiredStorage = requiredStorage(rawSize, userdataBytes)

        evaluateDevice(device, findings)
        evaluateStructure(facts, rawSize, findings)
        evaluateAndroidVersion(facts, device, findings)
        evaluateAbi(facts, device, findings)
        evaluateVndk(facts, device, findings)
        evaluateStorage(device, requiredStorage, rawSize, findings)
        evaluateExtras(facts, device, findings)

        val storageLimited = findings.any {
            it.code == CompatibilityCode.STORAGE_LOW || it.code == CompatibilityCode.STORAGE_GSID_THRESHOLD
        }
        findings += CompatibilityFinding(
            severity = FindingSeverity.INFO,
            code = if (storageLimited) CompatibilityCode.DATA_LIMITED else CompatibilityCode.DATA_SAFE,
        )

        val blockerCount = findings.count { it.severity == FindingSeverity.BLOCKER }
        val warningCount = findings.count { it.severity == FindingSeverity.WARNING }
        val level = when {
            blockerCount > 0 -> CompatibilityLevel.INCOMPATIBLE
            warningCount == 0 -> CompatibilityLevel.COMPATIBLE
            warningCount <= 2 -> CompatibilityLevel.LIKELY_COMPATIBLE
            else -> CompatibilityLevel.RISKY
        }
        val structuralFailure = findings.any {
            it.severity == FindingSeverity.BLOCKER && it.code in STRUCTURAL_CODES
        }
        val chance = when {
            structuralFailure -> 5
            level == CompatibilityLevel.INCOMPATIBLE -> 10
            level == CompatibilityLevel.RISKY -> 40
            level == CompatibilityLevel.LIKELY_COMPATIBLE -> 70
            else -> 90
        }

        return GsiCompatibilityReport(
            level = level,
            estimatedBootChancePercent = chance,
            findings = findings.sortedWith(compareBy({ it.severity.ordinal }, { it.code.ordinal })),
            facts = facts,
            device = device,
            requiredStorageBytes = requiredStorage,
            freeStorageBytes = device.freeDataBytes,
            dataSafety = if (storageLimited) DataSafety.LIMITED else DataSafety.SAFE,
            imageLabel = imageLabel,
        )
    }

    private fun evaluateDevice(device: DeviceProfile, findings: MutableList<CompatibilityFinding>) {
        if (device.sdk < 29) {
            findings += blocker(CompatibilityCode.DEVICE_ANDROID_TOO_OLD, device.sdk.toString())
        }
        if (device.firstApiLevel != null && device.firstApiLevel < 28) {
            findings += warning(
                CompatibilityCode.DEVICE_LAUNCHED_BEFORE_TREBLE,
                device.firstApiLevel.toString(),
            )
        }
        when (device.trebleEnabled) {
            false -> findings += blocker(CompatibilityCode.DEVICE_NOT_TREBLE)
            null -> findings += info(CompatibilityCode.DEVICE_TREBLE_UNKNOWN)
            true -> Unit
        }
        if (!device.dynamicPartitions) {
            findings += blocker(CompatibilityCode.DEVICE_NO_DYNAMIC_PARTITIONS)
        }
        if (device.dsuRunning) {
            findings += blocker(CompatibilityCode.DEVICE_DSU_RUNNING)
        } else if (device.dsuInstalled) {
            findings += warning(CompatibilityCode.DEVICE_DSU_INSTALLED)
        }
        if (device.bootloaderLocked == true) {
            findings += warning(CompatibilityCode.DEVICE_BOOTLOADER_LOCKED)
        }
    }

    private fun evaluateStructure(
        facts: GsiFacts,
        rawSize: Long,
        findings: MutableList<CompatibilityFinding>,
    ) {
        if (facts.error != null) {
            findings += blocker(CompatibilityCode.IMAGE_READ_ERROR, facts.error.orEmpty())
            return
        }
        if (facts.hasGpt) {
            findings += blocker(CompatibilityCode.IMAGE_FULL_DISK)
            return
        }
        if (facts.hasSuper) {
            findings += blocker(CompatibilityCode.IMAGE_SUPER)
            return
        }
        if (facts.hasBootMagic) {
            findings += blocker(CompatibilityCode.IMAGE_BOOT)
            return
        }
        if (rawSize <= 0L) {
            findings += blocker(CompatibilityCode.IMAGE_EMPTY)
            return
        }
        when (facts.fileSystem) {
            ImageFileSystem.EXT4 -> findings += pass(CompatibilityCode.IMAGE_FORMAT_EXT4)
            ImageFileSystem.EROFS -> findings += pass(CompatibilityCode.IMAGE_FORMAT_EROFS)
            ImageFileSystem.F2FS -> findings += warning(
                CompatibilityCode.IMAGE_FORMAT_F2FS,
            )
            ImageFileSystem.UNKNOWN -> {
                if (facts.container == ImageContainer.ZIP && facts.zipImages.isEmpty()) {
                    findings += blocker(CompatibilityCode.ZIP_NO_SYSTEM_IMAGE)
                } else {
                    findings += blocker(CompatibilityCode.IMAGE_FORMAT_UNKNOWN)
                }
            }
        }
        if (rawSize % 1024L != 0L) {
            findings += warning(CompatibilityCode.IMAGE_SIZE_NOT_ALIGNED)
        }
        if (facts.sizeMismatch) {
            findings += warning(CompatibilityCode.IMAGE_SIZE_MISMATCH)
        }
        if (facts.sparse) {
            findings += info(CompatibilityCode.IMAGE_FORMAT_SPARSE)
        }
        facts.compressionLabel?.let { compression ->
            if (facts.container != ImageContainer.ZIP) {
                findings += info(CompatibilityCode.IMAGE_FORMAT_COMPRESSED, compression)
            }
        }
        if (facts.container == ImageContainer.ZIP) {
            findings += pass(CompatibilityCode.IMAGE_FORMAT_ZIP)
            if (facts.zipImages.size > 1) {
                findings += info(
                    CompatibilityCode.ZIP_MULTIPLE_PARTITIONS,
                    facts.zipImages.joinToString(", "),
                )
            }
        }
    }

    private fun evaluateAndroidVersion(
        facts: GsiFacts,
        device: DeviceProfile,
        findings: MutableList<CompatibilityFinding>,
    ) {
        val sdk = facts.sdk
        if (sdk == null) {
            findings += warning(
                if (facts.props.isEmpty()) {
                    CompatibilityCode.IMAGE_PROPS_UNAVAILABLE
                } else {
                    CompatibilityCode.SDK_UNKNOWN
                },
            )
            when (device.crossVersionSupported) {
                true -> findings += info(CompatibilityCode.DEVICE_CROSS_VERSION_SUPPORTED)
                false -> findings += info(
                    CompatibilityCode.DEVICE_CROSS_VERSION_UNSUPPORTED,
                    device.sdk.toString(),
                )
                null -> findings += info(CompatibilityCode.DEVICE_CROSS_VERSION_UNKNOWN)
            }
            return
        }
        if (sdk < 28) {
            findings += blocker(CompatibilityCode.SDK_TOO_OLD, sdk.toString())
            return
        }
        when (device.crossVersionSupported) {
            true -> {
                findings += pass(CompatibilityCode.DEVICE_CROSS_VERSION_SUPPORTED)
                when {
                    sdk < device.sdk -> findings += warning(
                        CompatibilityCode.SDK_OLDER_THAN_DEVICE,
                        sdk.toString(),
                        device.sdk.toString(),
                    )

                    sdk > device.sdk -> findings += info(
                        CompatibilityCode.SDK_NEWER,
                        sdk.toString(),
                        device.sdk.toString(),
                    )

                    else -> findings += pass(
                        CompatibilityCode.SDK_SUPPORTED,
                        sdk.toString(),
                        device.sdk.toString(),
                    )
                }
            }

            false -> {
                findings += info(
                    CompatibilityCode.DEVICE_CROSS_VERSION_UNSUPPORTED,
                    device.sdk.toString(),
                )
                if (sdk == device.sdk) {
                    findings += pass(
                        CompatibilityCode.SDK_SUPPORTED,
                        sdk.toString(),
                        device.sdk.toString(),
                    )
                } else {
                    findings += blocker(
                        CompatibilityCode.SDK_VERSION_MISMATCH,
                        device.sdk.toString(),
                        sdk.toString(),
                    )
                }
            }

            null -> {
                findings += info(CompatibilityCode.DEVICE_CROSS_VERSION_UNKNOWN)
                when {
                    sdk < device.sdk -> findings += blocker(
                        CompatibilityCode.SDK_OLDER_THAN_DEVICE,
                        sdk.toString(),
                        device.sdk.toString(),
                    )

                    sdk > device.sdk -> findings += warning(
                        CompatibilityCode.SDK_NEWER_UNVERIFIED,
                        sdk.toString(),
                        device.sdk.toString(),
                    )

                    else -> findings += pass(
                        CompatibilityCode.SDK_SUPPORTED,
                        sdk.toString(),
                        device.sdk.toString(),
                    )
                }
            }
        }
    }

    private fun evaluateAbi(
        facts: GsiFacts,
        device: DeviceProfile,
        findings: MutableList<CompatibilityFinding>,
    ) {
        val abi = facts.abi
        if (abi == null) {
            findings += warning(CompatibilityCode.ABI_UNKNOWN)
            return
        }
        if (device.supportedAbis.any { it.equals(abi, ignoreCase = true) }) {
            findings += pass(CompatibilityCode.ABI_SUPPORTED, abi)
        } else {
            findings += blocker(
                CompatibilityCode.ABI_MISMATCH,
                abi,
                device.supportedAbis.joinToString(", ").ifBlank { "unknown" },
            )
        }
    }

    private fun evaluateVndk(
        facts: GsiFacts,
        device: DeviceProfile,
        findings: MutableList<CompatibilityFinding>,
    ) {
        // Cross-version support is the authoritative signal. VNDK numbers are
        // only compared when both sides expose a usable value.
        val gsiVndk = majorVersion(facts.vndkVersion) ?: return
        val deviceVndk = majorVersion(device.vndkVersion) ?: return
        if (gsiVndk == deviceVndk) {
            findings += pass(CompatibilityCode.VNDK_SUPPORTED, gsiVndk.toString())
        } else if (device.crossVersionSupported != true) {
            findings += warning(
                CompatibilityCode.VNDK_MISMATCH,
                gsiVndk.toString(),
                deviceVndk.toString(),
            )
        }
    }

    private fun evaluateStorage(
        device: DeviceProfile,
        requiredStorage: Long,
        rawSize: Long,
        findings: MutableList<CompatibilityFinding>,
    ) {
        if (rawSize <= 0L) return
        if (device.freeDataBytes < requiredStorage) {
            findings += blocker(
                CompatibilityCode.STORAGE_LOW,
                formatBytes(requiredStorage),
                formatBytes(device.freeDataBytes),
            )
        } else {
            findings += pass(
                CompatibilityCode.STORAGE_OK,
                formatBytes(device.freeDataBytes),
                formatBytes(requiredStorage),
            )
        }
        if (device.totalDataBytes > 0L && device.freeDataBytes >= requiredStorage) {
            val projectedFree = (device.freeDataBytes - requiredStorage).coerceAtLeast(0L)
            val projectedFraction = projectedFree.toFloat() / device.totalDataBytes.toFloat()
            if (projectedFraction <= device.gsidMinAllocFraction) {
                findings += warning(
                    CompatibilityCode.STORAGE_GSID_THRESHOLD,
                    formatPercent(device.gsidMinAllocFraction),
                    formatPercent(projectedFraction),
                )
            }
        }
    }

    private fun evaluateExtras(
        facts: GsiFacts,
        device: DeviceProfile,
        findings: MutableList<CompatibilityFinding>,
    ) {
        val gsiPatch = facts.securityPatch
        val devicePatch = device.securityPatch
        if (gsiPatch != null && devicePatch != null &&
            gsiPatch.length == 10 && devicePatch.length == 10 &&
            gsiPatch < devicePatch
        ) {
            findings += info(CompatibilityCode.SECURITY_PATCH_OLDER, gsiPatch, devicePatch)
        }
        if (facts.buildType in setOf("userdebug", "eng")) {
            findings += info(CompatibilityCode.BUILD_TYPE_UNOFFICIAL, facts.buildType ?: "")
        }
    }

    private fun requiredStorage(rawSize: Long, userdataBytes: Long): Long {
        val userdata = max(0L, userdataBytes)
        return safeAdd(safeAdd(rawSize, userdata), STORAGE_MARGIN_BYTES)
    }

    private fun safeAdd(first: Long, second: Long): Long {
        if (second > 0L && first > Long.MAX_VALUE - second) return Long.MAX_VALUE
        return first + second
    }

    private fun blocker(code: CompatibilityCode, vararg args: String) =
        CompatibilityFinding(FindingSeverity.BLOCKER, code, args.toList())

    private fun warning(code: CompatibilityCode, vararg args: String) =
        CompatibilityFinding(FindingSeverity.WARNING, code, args.toList())

    private fun info(code: CompatibilityCode, vararg args: String) =
        CompatibilityFinding(FindingSeverity.INFO, code, args.toList())

    private fun pass(code: CompatibilityCode, vararg args: String) =
        CompatibilityFinding(FindingSeverity.PASS, code, args.toList())

    private fun majorVersion(value: String?): Int? {
        if (value.isNullOrBlank()) return null
        val cleaned = value.trim().removePrefix("v").substringBefore('.')
        return cleaned.toIntOrNull()?.takeIf { it > 0 }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) return String.format(Locale.US, "%.1f GB", gb)
        val mb = bytes / (1024.0 * 1024.0)
        if (mb >= 1.0) return String.format(Locale.US, "%.0f MB", mb)
        return String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    }

    private fun formatPercent(fraction: Float): String {
        return String.format(Locale.US, "%.0f", fraction * 100f)
    }

    private companion object {
        const val STORAGE_MARGIN_BYTES = 256L * 1024L * 1024L
        val STRUCTURAL_CODES = setOf(
            CompatibilityCode.IMAGE_READ_ERROR,
            CompatibilityCode.IMAGE_FULL_DISK,
            CompatibilityCode.IMAGE_SUPER,
            CompatibilityCode.IMAGE_BOOT,
            CompatibilityCode.IMAGE_EMPTY,
            CompatibilityCode.IMAGE_FORMAT_UNKNOWN,
            CompatibilityCode.ZIP_NO_SYSTEM_IMAGE,
            CompatibilityCode.ABI_MISMATCH,
        )
    }
}
