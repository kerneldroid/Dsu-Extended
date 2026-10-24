package com.dsu.extended.compat

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Read-only, seekable view over an image file. Implementations must be safe for
 * sequential and positional reads from one thread at a time.
 */
interface SeekableSource : Closeable {
    val size: Long

    fun readAt(
        offset: Long,
        destination: ByteArray,
        destinationOffset: Int = 0,
        length: Int = destination.size - destinationOffset,
    ): Int

    fun openStream(): InputStream

    fun readExactly(offset: Long, length: Int): ByteArray? {
        if (offset < 0L || length < 0 || offset >= size) return null
        val target = ByteArray(length)
        var read = 0
        while (read < length) {
            val count = readAt(offset + read, target, read, length - read)
            if (count <= 0) break
            read += count
        }
        return if (read == 0) null else target.copyOf(read)
    }
}

class ChannelSeekableSource(
    private val channel: FileChannel,
    private val closeable: Closeable,
) : SeekableSource {

    override val size: Long
        get() = channel.size()

    override fun readAt(
        offset: Long,
        destination: ByteArray,
        destinationOffset: Int,
        length: Int,
    ): Int {
        if (offset < 0L || length <= 0 || offset >= size) return -1
        val limit = minOf(length.toLong(), size - offset).toInt()
        if (limit <= 0) return -1
        val buffer = ByteBuffer.wrap(destination, destinationOffset, limit)
        var total = 0
        while (buffer.hasRemaining()) {
            val count = channel.read(buffer, offset + total)
            if (count <= 0) break
            total += count
        }
        return if (total == 0) -1 else total
    }

    override fun openStream(): InputStream {
        channel.position(0L)
        return object : InputStream() {
            private val single = ByteArray(1)
            private var closed = false

            override fun read(): Int {
                val count = read(single, 0, 1)
                return if (count < 0) -1 else single[0].toInt() and 0xFF
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (closed) throw IOException("stream closed")
                if (length == 0) return 0
                val target = ByteBuffer.wrap(buffer, offset, length)
                return channel.read(target)
            }

            override fun close() {
                closed = true
            }
        }
    }

    override fun close() {
        runCatching { closeable.close() }
    }
}

class GsiImageInput(
    val displayName: String,
    val storedSize: Long,
    val declaredRawSize: Long,
    private val seekable: SeekableSource?,
    private val streamFactory: () -> InputStream,
) {
    val seekableSource: SeekableSource?
        get() = seekable

    fun readHeader(length: Int = HEADER_BYTES): ByteArray {
        seekable?.readExactly(0L, length)?.let { return it }
        return streamFactory().use { it.readUpTo(length) }
    }

    fun openStream(): InputStream = streamFactory()

    private fun InputStream.readUpTo(length: Int): ByteArray {
        val buffer = ByteArray(length)
        var read = 0
        while (read < length) {
            val count = read(buffer, read, length - read)
            if (count <= 0) break
            read += count
        }
        return if (read == length) buffer else buffer.copyOf(read)
    }

    companion object {
        const val HEADER_BYTES = 8192
    }
}
