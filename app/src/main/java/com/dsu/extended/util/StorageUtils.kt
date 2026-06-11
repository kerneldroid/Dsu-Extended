package com.dsu.extended.util

import android.os.Environment
import android.os.StatFs

class StorageUtils {

    companion object {

        fun getAllocInfo(allowedPercentage: Float): Pair<Boolean, Int> {
            val statFs = StatFs(Environment.getDataDirectory().absolutePath)
            val blockSize = statFs.blockSizeLong
            val totalSize = statFs.blockCountLong * blockSize
            val availableSize = statFs.availableBlocksLong * blockSize
            val hasAvailableStorage =
                availableSize.toFloat() / totalSize.toFloat() > allowedPercentage
            var availSizeInGb =
                (availableSize / 1024L / 1024L / 1024L).toInt()

            // Reserve 4GB (4 is a totally arbitrary number).
            // Let say user pick a "img" file to install, this file, will be packed to "gz"
            // the new created file, will take some space, because of that, we reserve something here.
            // We may fix it depending on what user is installing in future.
            if (availSizeInGb >= 6) {
                availSizeInGb -= 4
            }

            val maximumAllowedForAllocation = availSizeInGb / 2
            return Pair(hasAvailableStorage, maximumAllowedForAllocation)
        }

        /**
         * Get free storage in GB
         */
        fun getFreeStorageGB(): Int {
            return try {
                val statFs = StatFs(Environment.getDataDirectory().absolutePath)
                val blockSize = statFs.blockSizeLong
                val availableSize = statFs.availableBlocksLong * blockSize
                (availableSize / 1024L / 1024L / 1024L).toInt()
            } catch (e: Exception) {
                0
            }
        }

        /**
         * Get total storage in GB
         */
        fun getTotalStorageGB(): Int {
            return try {
                val statFs = StatFs(Environment.getDataDirectory().absolutePath)
                val blockSize = statFs.blockSizeLong
                val totalSize = statFs.blockCountLong * blockSize
                (totalSize / 1024L / 1024L / 1024L).toInt()
            } catch (e: Exception) {
                0
            }
        }

        /**
         * Get used storage percentage
         */
        fun getUsedStoragePercentage(): Float {
            return try {
                val statFs = StatFs(Environment.getDataDirectory().absolutePath)
                val blockSize = statFs.blockSizeLong
                val totalSize = statFs.blockCountLong * blockSize
                val availableSize = statFs.availableBlocksLong * blockSize
                val usedSize = totalSize - availableSize
                (usedSize.toFloat() / totalSize.toFloat()) * 100
            } catch (e: Exception) {
                0f
            }
        }

        /**
         * Format size to human readable string
         */
        fun formatSize(bytes: Long): String {
            return when {
                bytes >= 1024L * 1024L * 1024L -> {
                    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
                    String.format("%.1f GB", gb)
                }
                bytes >= 1024L * 1024L -> {
                    val mb = bytes / (1024.0 * 1024.0)
                    String.format("%.1f MB", mb)
                }
                bytes >= 1024L -> {
                    val kb = bytes / 1024.0
                    String.format("%.1f KB", kb)
                }
                else -> "$bytes B"
            }
        }
    }
}
