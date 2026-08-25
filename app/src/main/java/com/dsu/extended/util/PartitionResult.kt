package com.dsu.extended.util

object PartitionResult {
    const val OK = 0
    const val BUSY = 1
    const val INVALID_NAME = 2
    const val DUPLICATE = 3
    const val NOT_FOUND = 4
    const val SIZE_INVALID = 5
    const val NO_SPACE = 6
    const val SERVICE_UNAVAILABLE = 7
    const val CANCELLED = 8
    const val IO_ERROR = 9

    const val COPY_BUFFER_BYTES = 4 * 1024 * 1024
    const val BYTES_PER_PROGRESS_STEP = 32L * 1024 * 1024
    const val MAP_TIMEOUT_MS = 10_000
}
