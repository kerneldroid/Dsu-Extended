package com.dsu.extended.compat

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * Seekable view over the logical content of an Android sparse image.
 *
 * Only the chunk table is read while indexing; chunk payloads are skipped by
 * seeking in the container. Random reads (superblock, group descriptors, inode
 * tables, build.prop) therefore do not require decoding the whole image, which
 * is what made validation of multi-gigabyte sparse GSIs take minutes.
 *
 * The container stays owned by the caller; [close] is a no-op.
 */
internal class SparseSeekableSource(
    private val container: SeekableSource,
    private val budget: ScanBudget = ScanBudget(),
) : SeekableSource {

    private class Segment(
        val logicalStart: Long,
        val length: Long,
        val physicalStart: Long,
        val type: Int,
        val fill: Int = 0,
    )

    private val segments = ArrayList<Segment>()

    override val size: Long

    init {
        val header = container.readExactly(0L, HEADER_SIZE)
            ?: throw IOException("sparse 镜像头部被截断")
        if (header.size < HEADER_SIZE) throw IOException("sparse 镜像头部被截断")
        if (le32(header, 0) != SPARSE_MAGIC) throw IOException("不是 sparse 镜像")
        if (le16(header, 4) > 1) throw IOException("不支持的 sparse 版本")
        if (le16(header, 8) != HEADER_SIZE) throw IOException("sparse 头部尺寸无效")
        if (le16(header, 10) != CHUNK_HEADER_SIZE) throw IOException("sparse 数据块头部尺寸无效")

        val blockSize = le32(header, 12).toLong() and 0xFFFFFFFFL
        if (blockSize <= 0L || blockSize % 4L != 0L) throw IOException("sparse 块大小无效")
        val totalBlocks = le32(header, 16).toLong() and 0xFFFFFFFFL
        val totalChunks = le32(header, 20).toLong() and 0xFFFFFFFFL
        if (totalBlocks <= 0L || totalChunks <= 0L) throw IOException("sparse 镜像为空")
        if (totalChunks > MAX_SEGMENTS) throw IOException("sparse 镜像数据块过多")
        if (totalBlocks > Long.MAX_VALUE / blockSize) throw IOException("sparse 镜像大小无效")

        val declaredSize = totalBlocks * blockSize
        var physical = HEADER_SIZE.toLong()
        var logical = 0L
        var index = 0L
        while (index < totalChunks) {
            budget.check()
            val chunkHeader = container.readExactly(physical, CHUNK_HEADER_SIZE)
                ?: throw EOFException("sparse 数据块头部被截断")
            physical += CHUNK_HEADER_SIZE
            val type = le16(chunkHeader, 0)
            val chunkBlocks = le32(chunkHeader, 4).toLong() and 0xFFFFFFFFL
            val dataSize = le32(chunkHeader, 8).toLong() and 0xFFFFFFFFL
            val chunkBytes = chunkBlocks * blockSize
            when (type) {
                CHUNK_RAW -> {
                    if (chunkBytes <= 0L || dataSize != chunkBytes) {
                        throw IOException("sparse 原始数据块无效")
                    }
                    segments += Segment(logical, chunkBytes, physical, type)
                    physical += dataSize
                }

                CHUNK_FILL -> {
                    if (chunkBytes <= 0L || dataSize != 4L) throw IOException("sparse 填充数据块无效")
                    val fill = container.readExactly(physical, 4)
                        ?: throw EOFException("sparse 填充数据块被截断")
                    segments += Segment(logical, chunkBytes, 0L, type, le32(fill, 0))
                    physical += 4L
                }

                CHUNK_DONT_CARE -> {
                    if (chunkBytes <= 0L) throw IOException("sparse dont-care 数据块无效")
                    segments += Segment(logical, chunkBytes, 0L, type)
                    physical += dataSize
                }

                CHUNK_CRC32 -> physical += dataSize

                else -> throw IOException("不支持的 sparse 数据块类型：$type")
            }
            logical += chunkBytes
            if (logical < 0L || logical > declaredSize) throw IOException("sparse 数据块超出镜像大小")
            index++
        }
        size = logical
        if (size != declaredSize) throw IOException("sparse 镜像大小不匹配")
        if (segments.isEmpty()) throw IOException("sparse 镜像为空")
    }

    override fun readAt(
        offset: Long,
        destination: ByteArray,
        destinationOffset: Int,
        length: Int,
    ): Int {
        if (offset < 0L || length <= 0 || offset >= size) return -1
        if (destinationOffset < 0 || destinationOffset + length > destination.size) {
            throw IndexOutOfBoundsException("目标区间无效")
        }
        var logical = offset
        var written = 0
        while (written < length && logical < size) {
            val segment = segmentAt(logical) ?: break
            val inSegment = logical - segment.logicalStart
            val count = minOf(segment.length - inSegment, (length - written).toLong()).toInt()
            if (count <= 0) break
            when (segment.type) {
                CHUNK_RAW -> {
                    val read = container.readAt(
                        segment.physicalStart + inSegment,
                        destination,
                        destinationOffset + written,
                        count,
                    )
                    if (read <= 0) break
                    written += read
                    logical += read
                    if (read < count) break
                }

                CHUNK_FILL -> {
                    for (position in 0 until count) {
                        val patternIndex = ((inSegment + position) and 3L).toInt()
                        destination[destinationOffset + written + position] =
                            ((segment.fill ushr (patternIndex * 8)) and 0xFF).toByte()
                    }
                    written += count
                    logical += count
                }

                else -> {
                    java.util.Arrays.fill(
                        destination,
                        destinationOffset + written,
                        destinationOffset + written + count,
                        0,
                    )
                    written += count
                    logical += count
                }
            }
        }
        return if (written == 0) -1 else written
    }

    override fun openStream(): InputStream = object : InputStream() {
        private val single = ByteArray(1)
        private var position = 0L

        override fun read(): Int {
            val count = read(single, 0, 1)
            return if (count < 0) -1 else single[0].toInt() and 0xFF
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (position >= size) return -1
            val count = readAt(position, buffer, offset, length)
            if (count > 0) position += count
            return count
        }

        override fun skip(count: Long): Long {
            if (count <= 0L) return 0L
            val target = minOf(position + count, size)
            val skipped = target - position
            position = target
            return skipped
        }
    }

    override fun close() = Unit

    private fun segmentAt(offset: Long): Segment? {
        var low = 0
        var high = segments.size - 1
        var found = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (segments[middle].logicalStart <= offset) {
                found = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        if (found < 0) return null
        val segment = segments[found]
        return if (offset < segment.logicalStart + segment.length) segment else null
    }

    private companion object {
        const val HEADER_SIZE = 28
        const val CHUNK_HEADER_SIZE = 12
        const val CHUNK_RAW = 0xCAC1
        const val CHUNK_FILL = 0xCAC2
        const val CHUNK_DONT_CARE = 0xCAC3
        const val CHUNK_CRC32 = 0xCAC4
        const val MAX_SEGMENTS = 262_144L
        const val SPARSE_MAGIC = 0xED26FF3A.toInt()

        fun le16(bytes: ByteArray, offset: Int): Int {
            if (offset < 0 || offset + 2 > bytes.size) return 0
            return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
        }

        fun le32(bytes: ByteArray, offset: Int): Int {
            if (offset < 0 || offset + 4 > bytes.size) return 0
            return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
        }
    }
}
