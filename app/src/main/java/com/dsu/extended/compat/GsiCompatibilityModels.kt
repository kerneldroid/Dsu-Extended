package com.dsu.extended.compat

enum class ImageContainer {
    RAW,
    GZIP,
    XZ,
    ZIP,
}

enum class ImageFileSystem {
    EXT4,
    EROFS,
    F2FS,
    UNKNOWN,
}

enum class CompatibilityLevel {
    COMPATIBLE,
    LIKELY_COMPATIBLE,
    RISKY,
    INCOMPATIBLE,
}

enum class FindingSeverity {
    BLOCKER,
    WARNING,
    INFO,
    PASS,
}

enum class DataSafety {
    SAFE,
    LIMITED,
}

/**
 * Stable identifiers for every check. UI text is resolved from these codes, so the
 * evaluator stays free of Android resources and can be unit tested on the JVM.
 */
enum class CompatibilityCode {
    DEVICE_ANDROID_TOO_OLD,
    DEVICE_LAUNCHED_BEFORE_TREBLE,
    DEVICE_NOT_TREBLE,
    DEVICE_TREBLE_UNKNOWN,
    DEVICE_NO_DYNAMIC_PARTITIONS,
    DEVICE_DSU_RUNNING,
    DEVICE_DSU_INSTALLED,
    DEVICE_BOOTLOADER_LOCKED,
    DEVICE_CROSS_VERSION_SUPPORTED,
    DEVICE_CROSS_VERSION_UNSUPPORTED,
    DEVICE_CROSS_VERSION_UNKNOWN,

    ABI_SUPPORTED,
    ABI_MISMATCH,
    ABI_UNKNOWN,

    SDK_SUPPORTED,
    SDK_NEWER,
    SDK_NEWER_UNVERIFIED,
    SDK_OLDER_THAN_DEVICE,
    SDK_VERSION_MISMATCH,
    SDK_TOO_OLD,
    SDK_UNKNOWN,

    VNDK_SUPPORTED,
    VNDK_MISMATCH,

    IMAGE_FORMAT_EXT4,
    IMAGE_FORMAT_EROFS,
    IMAGE_FORMAT_F2FS,
    IMAGE_FORMAT_UNKNOWN,
    IMAGE_FORMAT_SPARSE,
    IMAGE_FORMAT_COMPRESSED,
    IMAGE_FORMAT_ZIP,
    IMAGE_FULL_DISK,
    IMAGE_SUPER,
    IMAGE_BOOT,
    IMAGE_EMPTY,
    IMAGE_SIZE_NOT_ALIGNED,
    IMAGE_SIZE_MISMATCH,
    IMAGE_PROPS_UNAVAILABLE,
    IMAGE_READ_ERROR,
    ZIP_NO_SYSTEM_IMAGE,
    ZIP_MULTIPLE_PARTITIONS,

    STORAGE_LOW,
    STORAGE_GSID_THRESHOLD,
    STORAGE_OK,

    DATA_SAFE,
    DATA_LIMITED,

    SECURITY_PATCH_OLDER,
    BUILD_TYPE_UNOFFICIAL,
}

data class GsiFacts(
    val container: ImageContainer = ImageContainer.RAW,
    val fileSystem: ImageFileSystem = ImageFileSystem.UNKNOWN,
    val sparse: Boolean = false,
    val rawSizeBytes: Long = 0L,
    val storedSizeBytes: Long = 0L,
    val declaredRawSizeBytes: Long = 0L,
    val props: Map<String, String> = emptyMap(),
    val zipImages: List<String> = emptyList(),
    val hasGpt: Boolean = false,
    val hasSuper: Boolean = false,
    val hasBootMagic: Boolean = false,
    val propsSource: String? = null,
    val scanComplete: Boolean = true,
    val sizeMismatch: Boolean = false,
    val error: String? = null,
) {
    val effectiveRawSize: Long
        get() = rawSizeBytes.takeIf { it > 0L } ?: declaredRawSizeBytes

    val compressionLabel: String?
        get() = when (container) {
            ImageContainer.GZIP -> "gzip"
            ImageContainer.XZ -> "xz"
            ImageContainer.ZIP -> "zip"
            ImageContainer.RAW -> null
        }

    val sdk: Int?
        get() = intProp("ro.build.version.sdk") ?: intProp("ro.system.build.version.sdk")

    val androidVersion: String?
        get() = prop("ro.build.version.release")
            ?: prop("ro.system.build.version.release")
            ?: prop("ro.build.version.release_or_codename")

    val abi: String?
        get() = prop("ro.product.cpu.abi") ?: abiList?.firstOrNull()

    val abiList: List<String>?
        get() = prop("ro.product.cpu.abilist")
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }

    val vndkVersion: String?
        get() = prop("ro.vndk.version") ?: prop("ro.vndk.lite")

    val trebleEnabled: Boolean?
        get() = prop("ro.treble.enabled")?.toBooleanStrictOrNull()

    val buildType: String?
        get() = prop("ro.build.type") ?: prop("ro.system.build.type")

    val securityPatch: String?
        get() = prop("ro.build.version.security_patch")

    val fingerprint: String?
        get() = prop("ro.build.fingerprint") ?: prop("ro.system.build.fingerprint")

    val deviceName: String?
        get() = prop("ro.product.device") ?: prop("ro.product.name")

    private fun prop(key: String): String? = props[key]?.trim()?.takeIf { it.isNotEmpty() }

    private fun intProp(key: String): Int? = prop(key)?.toIntOrNull()
}

data class DeviceProfile(
    val sdk: Int,
    val androidVersion: String,
    val model: String,
    val manufacturer: String,
    val supportedAbis: List<String>,
    val trebleEnabled: Boolean?,
    val crossVersionSupported: Boolean?,
    val firstApiLevel: Int?,
    val vndkVersion: String?,
    val dynamicPartitions: Boolean,
    val bootloaderLocked: Boolean?,
    val freeDataBytes: Long,
    val totalDataBytes: Long,
    val gsidMinAllocFraction: Float,
    val dsuInstalled: Boolean,
    val dsuRunning: Boolean,
    val securityPatch: String?,
) {
    val displayName: String
        get() = listOf(manufacturer, model).filter { it.isNotBlank() }.joinToString(" ").trim()

    val freeDataFraction: Float
        get() = if (totalDataBytes > 0L) freeDataBytes.toFloat() / totalDataBytes.toFloat() else 0f
}

data class CompatibilityFinding(
    val severity: FindingSeverity,
    val code: CompatibilityCode,
    val args: List<String> = emptyList(),
)

data class GsiCompatibilityReport(
    val level: CompatibilityLevel,
    val estimatedBootChancePercent: Int,
    val findings: List<CompatibilityFinding>,
    val facts: GsiFacts,
    val device: DeviceProfile,
    val requiredStorageBytes: Long,
    val freeStorageBytes: Long,
    val dataSafety: DataSafety,
    val imageLabel: String,
    val checkedAtMillis: Long = System.currentTimeMillis(),
) {
    val blockers: List<CompatibilityFinding>
        get() = findings.filter { it.severity == FindingSeverity.BLOCKER }

    val warnings: List<CompatibilityFinding>
        get() = findings.filter { it.severity == FindingSeverity.WARNING }

    val passed: List<CompatibilityFinding>
        get() = findings.filter { it.severity == FindingSeverity.PASS }
}
