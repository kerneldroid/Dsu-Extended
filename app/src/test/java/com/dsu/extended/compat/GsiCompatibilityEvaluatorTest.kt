package com.dsu.extended.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GsiCompatibilityEvaluatorTest {

    private val device = DeviceProfile(
        sdk = 34,
        androidVersion = "14",
        model = "Test Device",
        manufacturer = "Test",
        supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        trebleEnabled = true,
        crossVersionSupported = true,
        firstApiLevel = 31,
        vndkVersion = "34",
        dynamicPartitions = true,
        bootloaderLocked = false,
        freeDataBytes = 100L * 1024L * 1024L * 1024L,
        totalDataBytes = 128L * 1024L * 1024L * 1024L,
        gsidMinAllocFraction = 0.10f,
        dsuInstalled = false,
        dsuRunning = false,
        securityPatch = "2026-09-01",
    )

    private fun facts(
        sdk: Int = 34,
        abi: String = "arm64-v8a",
        fileSystem: ImageFileSystem = ImageFileSystem.EXT4,
        rawSize: Long = 4L * 1024L * 1024L * 1024L,
        hasGpt: Boolean = false,
    ): GsiFacts {
        return GsiFacts(
            container = ImageContainer.RAW,
            fileSystem = fileSystem,
            rawSizeBytes = rawSize,
            storedSizeBytes = rawSize,
            declaredRawSizeBytes = rawSize,
            hasGpt = hasGpt,
            props = mapOf(
                "ro.build.version.sdk" to sdk.toString(),
                "ro.build.version.release" to "Android",
                "ro.product.cpu.abi" to abi,
                "ro.vndk.version" to "34",
                "ro.build.type" to "user",
                "ro.build.version.security_patch" to "2026-09-01",
            ),
            propsSource = "test",
        )
    }

    @Test
    fun compatibleImagePasses() {
        val report = GsiCompatibilityEvaluator().evaluate(facts(), device, 2L * 1024L * 1024L * 1024L)
        assertEquals(CompatibilityLevel.COMPATIBLE, report.level)
        assertEquals(90, report.estimatedBootChancePercent)
        assertTrue(report.blockers.isEmpty())
        assertTrue(report.findings.any { it.code == CompatibilityCode.ABI_SUPPORTED })
        assertTrue(report.findings.any { it.code == CompatibilityCode.DATA_SAFE })
    }

    @Test
    fun abiMismatchIsBlocker() {
        val report = GsiCompatibilityEvaluator().evaluate(
            facts(abi = "x86_64"),
            device,
            2L * 1024L * 1024L * 1024L,
        )
        assertEquals(CompatibilityLevel.INCOMPATIBLE, report.level)
        assertTrue(report.blockers.any { it.code == CompatibilityCode.ABI_MISMATCH })
    }

    @Test
    fun olderGsiWithoutCrossVersionIsBlocker() {
        val report = GsiCompatibilityEvaluator().evaluate(
            facts(sdk = 33),
            device.copy(crossVersionSupported = false),
            2L * 1024L * 1024L * 1024L,
        )
        assertEquals(CompatibilityLevel.INCOMPATIBLE, report.level)
        assertTrue(report.blockers.any { it.code == CompatibilityCode.SDK_VERSION_MISMATCH })
    }

    @Test
    fun newerGsiWithCrossVersionPasses() {
        val report = GsiCompatibilityEvaluator().evaluate(
            facts(sdk = 35),
            device,
            2L * 1024L * 1024L * 1024L,
        )
        assertEquals(CompatibilityLevel.COMPATIBLE, report.level)
        assertTrue(report.findings.any { it.code == CompatibilityCode.SDK_NEWER })
    }

    @Test
    fun lowStorageIsBlocker() {
        val report = GsiCompatibilityEvaluator().evaluate(
            facts(),
            device.copy(freeDataBytes = 1L * 1024L * 1024L * 1024L),
            2L * 1024L * 1024L * 1024L,
        )
        assertEquals(CompatibilityLevel.INCOMPATIBLE, report.level)
        assertTrue(report.blockers.any { it.code == CompatibilityCode.STORAGE_LOW })
        assertEquals(DataSafety.LIMITED, report.dataSafety)
    }

    @Test
    fun fullDiskImageIsStructuralFailure() {
        val report = GsiCompatibilityEvaluator().evaluate(
            facts(hasGpt = true),
            device,
            2L * 1024L * 1024L * 1024L,
        )
        assertEquals(CompatibilityLevel.INCOMPATIBLE, report.level)
        assertEquals(5, report.estimatedBootChancePercent)
        assertTrue(report.blockers.any { it.code == CompatibilityCode.IMAGE_FULL_DISK })
    }

    @Test
    fun unknownPropsAreWarningsNotBlockers() {
        val report = GsiCompatibilityEvaluator().evaluate(
            facts().copy(props = emptyMap()),
            device,
            2L * 1024L * 1024L * 1024L,
        )
        assertTrue(report.blockers.isEmpty())
        assertTrue(report.warnings.any { it.code == CompatibilityCode.IMAGE_PROPS_UNAVAILABLE })
        assertTrue(report.warnings.any { it.code == CompatibilityCode.ABI_UNKNOWN })
    }
}
