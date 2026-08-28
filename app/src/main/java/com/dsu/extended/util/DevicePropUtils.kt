package com.dsu.extended.util

import android.os.Build
import android.os.SystemProperties

class DevicePropUtils {

    companion object {
        fun getGsidBinaryAllowedPerc(): Float {
            val tag = DevicePropUtils::class.java.simpleName
            val fallback = if (Build.VERSION.SDK_INT >= 35) 0.10F else 0.40F
            val minAllowed = getSystemProperty("ro.com.dsu.extended.gsid_min_alloc")

            if (minAllowed.isEmpty()) {
                AppLogger.d(
                    tag,
                    "gsid_min_alloc is not set, using fallback",
                    "fallback" to fallback,
                    "sdk" to Build.VERSION.SDK_INT,
                )
                return fallback
            }

            val parsed = minAllowed.toFloatOrNull()
            if (parsed == null) {
                AppLogger.w(
                    tag,
                    "gsid_min_alloc is invalid, using fallback",
                    "value" to minAllowed,
                    "fallback" to fallback,
                )
                return fallback
            }

            val clamped = parsed.coerceIn(0.01F, 0.95F)
            if (clamped != parsed) {
                AppLogger.w(
                    tag,
                    "gsid_min_alloc value was clamped",
                    "value" to parsed,
                    "clamped" to clamped,
                )
            }
            return clamped
        }

        fun hasDynamicPartitions(): Boolean {
            return getSystemProperty("ro.boot.dynamic_partitions") == "true"
        }

        fun isGsiInstalled(): Boolean {
            return getSystemProperty("ro.gsid.image_installed") == "1"
        }

        fun isGsiRunning(): Boolean {
            return getSystemProperty("ro.gsid.image_running") == "1"
        }

        private fun getSystemProperty(key: String): String {
            return try {
                SystemProperties.get(key)
            } catch (e: Exception) {
                AppLogger.e("DevicePropUtils", "Failed to get system property", e, "key" to key)
                ""
            }
        }
    }
}
