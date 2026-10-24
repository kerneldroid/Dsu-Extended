package com.dsu.extended.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GsiLogFilterTest {

    @Test
    fun keepsGsiDaemonAndDynamicSystemLines() {
        assertTrue(GsiLogFilter.shouldInclude("I/gsid: Installing system image"))
        assertTrue(
            GsiLogFilter.shouldInclude(
                "10-24 12:00:00.123  1000  1000 I DynamicSystemService: startInstallation",
            ),
        )
    }

    @Test
    fun dropsUnrelatedNoise() {
        assertFalse(GsiLogFilter.shouldInclude("I/ActivityManager: Start proc com.example.app"))
        assertFalse(GsiLogFilter.shouldInclude("D/WifiService: scan requested"))
    }

    @Test
    fun keepsNoisyTagsOnlyWhenRelevant() {
        assertTrue(GsiLogFilter.shouldInclude("I/init: Starting service 'gsid'"))
        assertTrue(GsiLogFilter.shouldInclude("I/vold: Mounting DSU userdata image"))
        assertFalse(GsiLogFilter.shouldInclude("I/init: starting service 'wificond'"))
    }

    @Test
    fun filtersMultilineSnapshot() {
        val filtered = GsiLogFilter.filter(
            """
            I/gsid: first
            I/WifiService: noise
            I/init: gsid started
            """.trimIndent(),
        )
        assertTrue(filtered.contains("first"))
        assertTrue(filtered.contains("gsid started"))
        assertFalse(filtered.contains("noise"))
    }
}
