package com.dsu.extended.compat

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream

/**
 * Streaming decoder for the Android sparse image format. Non-sparse input is
 * passed through unchanged, which lets callers use one code path for both.
 */
class SparseStream(input: InputStream) : InputStream() {

    private val source = PushbackInputStream(input, HEADER_SIZE)
    private val header = ByteArray(HEADER_SIZE)
    private var headerRead = 0
    val isSparse: Boolean
    val unsparseSize: Long

    private var blockSize = 0L
    private var totalChunks = 0L
    private var chunksRead = 0L
    private var remaining = 0L
    private var chunkType = 0
    private val fill = ByteArray(4)
    private var fillOffset = 0
    private var produced = 0L
    private var closed = false

    init {
        while (headerRead < HEADER_SIZE) {
            val count = source.read(header, headerRead, HEADER_SIZE - headerRead)
            if (count < 0) break
            headerRead += count
        }
        isSparse = headerRead >= 4 && le32(header, 0) == SPARSE_MAGIC
        if (!isSparse) {
            if (headerRead > 0) source.unread(header, 0, headerRead)
            unsparseSize = -1L
        } else {
            if (headerRead < HEADER_SIZE) throw EOFException("Truncated sparse header")
            val major = le16(header, 4)
            if (major > 1) throw IOException("Unsupported sparse version $major")
            if (le16(header, 8) != HEADER_SIZE) throw IOException("Invalid sparse header size")
            if (le16(header, 10) != CHUNK_HEADER_SIZE) throw IOException("Invalid sparse chunk header size")
            blockSize = le32(header, 12).toLong() and 0xFFFFFFFFL
            if (blockSize <= 0L || blockSize % 4L != 0L) throw IOException("Invalid sparse block size")
            val totalBlocks = le32(header, 16).toLong() and 0xFFFFFFFFL
            totalChunks = le32(header, 20).toLong() and 0xFFFFFFFFL
            if (totalBlocks <= 0L || totalChunks <= 0L) throw IOException("Empty sparse image")
            unsparseSize = totalBlocks * blockSize
        }
    }

    override fun read(): Int {
        val single = ByteArray(1)
        return if (read(single, 0, 1) < 0) -1 else single[0].toInt() and 0xFF
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (closed) throw IOException("stream closed")
        if (!isSparse) return source.read(buffer, offset, length)

        while (true) {
            if (remaining <= 0L) {
                if (!nextChunk()) {
                    if (produced < unsparseSize) {
                        throw EOFException("Sparse image ended early")
                    }
                    return -1
                }
            }
            val count = minOf(length.toLong(), remaining).toInt()
            when (chunkType) {
                CHUNK_RAW -> {
                    val read = source.read(buffer, offset, count)
                    if (read < 0) throw EOFException("Truncated sparse raw chunk")
                    remaining -= read
                    produced += read
                    return read
                }

                CHUNK_DONT_CARE -> {
                    java.util.Arrays.fill(buffer, offset, offset + count, 0)
                    remaining -= count
                    produced += count
                    return count
                }

                CHUNK_FILL -> {
                    val base = fillOffset
                    for (index in 0 until count) {
                        buffer[offset + index] = fill[(base + index) and 3]
                    }
                    fillOffset = (base + count) and 3
                    remaining -= count
                    produced += count
                    return count
                }

                else -> throw IOException("Unsupported sparse chunk type: $chunkType")
            }
        }
    }

    private fun nextChunk(): Boolean {
        while (chunksRead < totalChunks) {
            val chunkHeader = readFully(CHUNK_HEADER_SIZE)
            chunkType = le16(chunkHeader, 0)
            val chunkBlocks = le32(chunkHeader, 4).toLong() and 0xFFFFFFFFL
            chunksRead++
            when (chunkType) {
                CHUNK_RAW, CHUNK_DONT_CARE -> {
                    remaining = checkedChunkBytes(chunkBlocks)
                    fillOffset = 0
                }

                CHUNK_FILL -> {
                    readFully(4).copyInto(fill)
                    remaining = checkedChunkBytes(chunkBlocks)
                    fillOffset = 0
                }

                CHUNK_CRC32 -> {
                    readFully(4)
                    remaining = 0L
                    continue
                }

                else -> throw IOException("Unsupported sparse chunk type: $chunkType")
            }
            if (remaining <= 0L) continue
            return true
        }
        return false
    }

    private fun checkedChunkBytes(chunkBlocks: Long): Long {
        if (blockSize <= 0L || chunkBlocks > Long.MAX_VALUE / blockSize) {
            throw IOException("Invalid sparse chunk size")
        }
        val bytes = chunkBlocks * blockSize
        if (bytes > unsparseSize - produced) {
            throw IOException("Sparse chunk exceeds image size")
        }
        return bytes
    }

    private fun readFully(length: Int): ByteArray {
        val buffer = ByteArray(length)
        var read = 0
        while (read < length) {
            val count = source.read(buffer, read, length - read)
            if (count < 0) throw EOFException("Truncated sparse image")
            read += count
        }
        return buffer
    }

    override fun close() {
        closed = true
        source.close()
    }

    companion object {
        private const val HEADER_SIZE = 28
        private const val CHUNK_HEADER_SIZE = 12
        private const val SPARSE_MAGIC = 0xED26FF3A.toInt()
        private const val CHUNK_RAW = 0xCAC1
        private const val CHUNK_FILL = 0xCAC2
        private const val CHUNK_DONT_CARE = 0xCAC3
        private const val CHUNK_CRC32 = 0xCAC4

        internal fun le16(bytes: ByteArray, offset: Int): Int {
            if (offset + 2 > bytes.size) return 0
            return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
        }

        internal fun le32(bytes: ByteArray, offset: Int): Int {
            if (offset + 4 > bytes.size) return 0
            return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
        }
    }
}
