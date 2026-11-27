package com.dsu.extended.compat

import java.io.IOException
import java.io.InputStream

/**
 * Wall-clock budget for the compatibility scan.
 *
 * The check is advisory, so it must never keep the user on a spinner. Once the
 * budget is spent the analyzer stops reading and returns the facts gathered so
 * far instead of walking a multi-gigabyte image (or decompressing it) to the
 * end.
 */
class ScanBudget(val maxDurationMillis: Long = DEFAULT_MAX_DURATION_MS) {

    private val deadlineNanos = System.nanoTime() + maxDurationMillis * NANOS_PER_MILLI

    val isExpired: Boolean
        get() = System.nanoTime() - deadlineNanos >= 0L

    fun check() {
        if (isExpired) throw ExpiredException()
    }

    class ExpiredException : IOException("镜像分析超出时间预算")

    companion object {
        const val DEFAULT_MAX_DURATION_MS = 20_000L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}

/**
 * Fails reads once [budget] is spent. Decompressors (gzip, xz, zip) pull their
 * input through this wrapper, which is what stops archive enumeration that would
 * otherwise inflate gigabytes before the analyzer gets a chance to look at the
 * clock.
 */
class DeadlineInputStream(
    private val delegate: InputStream,
    private val budget: ScanBudget,
) : InputStream() {

    override fun read(): Int {
        budget.check()
        return delegate.read()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        budget.check()
        return delegate.read(buffer, offset, length)
    }

    override fun available(): Int = delegate.available()

    override fun skip(count: Long): Long {
        budget.check()
        return delegate.skip(count)
    }

    override fun close() = delegate.close()
}
