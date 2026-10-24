package com.dsu.extended.compat

import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Minimal read-only ext4 reader. It intentionally supports only what is needed
 * to locate and read build.prop: superblock parsing, group descriptors, inodes,
 * extent trees, direct/indirect blocks and directory entries. Corrupt or
 * unsupported structures are rejected instead of guessed.
 */
internal class Ext4Image(private val source: SeekableSource) {

    data class Info(
        val blockSize: Int,
        val blockCount: Long,
        val volumeLabel: String,
        val uuid: String,
        val featureIncompat: Long,
        val featureRoCompat: Long,
    ) {
        val rawSize: Long get() = blockCount * blockSize
        val hasExtents: Boolean get() = featureIncompat and INCOMPAT_EXTENTS != 0L
        val is64Bit: Boolean get() = featureIncompat and INCOMPAT_64BIT != 0L
        val metadataChecksum: Boolean get() = featureRoCompat and RO_COMPAT_METADATA_CSUM != 0L

        companion object {
            const val INCOMPAT_EXTENTS = 0x40L
            const val INCOMPAT_64BIT = 0x80L
            const val INCOMPAT_INLINE_DATA = 0x2000L
            const val RO_COMPAT_METADATA_CSUM = 0x400L
        }
    }

    private data class Inode(
        val number: Long,
        val mode: Int,
        val size: Long,
        val flags: Long,
        val block: ByteArray,
    ) {
        val isDirectory: Boolean get() = mode and MODE_TYPE == MODE_DIRECTORY
        val isRegular: Boolean get() = mode and MODE_TYPE == MODE_REGULAR
        val hasExtents: Boolean get() = flags and FLAG_EXTENTS != 0L
        val hasInlineData: Boolean get() = flags and FLAG_INLINE_DATA != 0L

        companion object {
            const val MODE_TYPE = 0xF000
            const val MODE_DIRECTORY = 0x4000
            const val MODE_REGULAR = 0x8000
            const val FLAG_EXTENTS = 0x80000L
            const val FLAG_INLINE_DATA = 0x10000000L
        }
    }

    private data class Extent(
        val logicalBlock: Long,
        val length: Long,
        val physicalBlock: Long,
        val unwritten: Boolean,
    )

    private var info: Info? = null
    private var inodesPerGroup = 0
    private var blocksPerGroup = 0
    private var inodeSize = 128
    private var descriptorSize = 32
    private var firstDataBlock = 0L
    private var is64Bit = false
    private var extentNodes = 0

    fun open(): Info? {
        val superblock = source.readExactly(SUPERBLOCK_OFFSET, SUPERBLOCK_SIZE) ?: return null
        if (superblock.size < SUPERBLOCK_SIZE) return null
        if (le16(superblock, OFFSET_MAGIC) != EXT4_MAGIC) return null

        val incompat = le32(superblock, OFFSET_FEATURE_INCOMPAT).toLong() and 0xFFFFFFFFL
        val roCompat = le32(superblock, OFFSET_FEATURE_RO_COMPAT).toLong() and 0xFFFFFFFFL
        val logBlockSize = le32(superblock, OFFSET_LOG_BLOCK_SIZE)
        if (logBlockSize < 0 || logBlockSize > 6) return null
        val blockSize = 1024 shl logBlockSize

        val inodeCount = le32(superblock, OFFSET_INODES_COUNT).toLong() and 0xFFFFFFFFL
        var blockCount = le32(superblock, OFFSET_BLOCKS_COUNT_LO).toLong() and 0xFFFFFFFFL
        is64Bit = incompat and Info.INCOMPAT_64BIT != 0L
        if (is64Bit) {
            blockCount = blockCount or
                ((le32(superblock, OFFSET_BLOCKS_COUNT_HI).toLong() and 0xFFFFFFFFL) shl 32)
        }
        inodesPerGroup = le32(superblock, OFFSET_INODES_PER_GROUP)
        blocksPerGroup = le32(superblock, OFFSET_BLOCKS_PER_GROUP)
        firstDataBlock = le32(superblock, OFFSET_FIRST_DATA_BLOCK).toLong() and 0xFFFFFFFFL
        val inodeBytes = le16(superblock, OFFSET_INODE_SIZE)
        inodeSize = if (inodeBytes == 0) 128 else inodeBytes
        descriptorSize = if (is64Bit) maxOf(64, le16(superblock, OFFSET_DESC_SIZE)) else 32

        if (inodeCount <= 0L || blockCount <= 0L) return null
        if (inodesPerGroup <= 0 || blocksPerGroup <= 0) return null
        if (inodeSize < 128 || inodeSize > blockSize) return null
        if (descriptorSize < 32) return null

        val uuid = (superblock.copyOfRange(OFFSET_UUID, OFFSET_UUID + 16)).toHex()
        val volumeLabel = String(
            superblock.copyOfRange(OFFSET_VOLUME_NAME, OFFSET_VOLUME_NAME + 16),
            StandardCharsets.UTF_8,
        ).trimEnd('\u0000').trim()

        val parsed = Info(
            blockSize = blockSize,
            blockCount = blockCount,
            volumeLabel = volumeLabel,
            uuid = uuid,
            featureIncompat = incompat,
            featureRoCompat = roCompat,
        )
        info = parsed
        return parsed
    }

    fun readBuildProps(): Map<String, String> {
        extentNodes = 0
        val merged = LinkedHashMap<String, String>()
        PROP_PATHS.forEach { path ->
            val bytes = runCatching { readFile(path) }.getOrNull() ?: return@forEach
            if (bytes.isEmpty()) return@forEach
            parseProps(bytes).forEach { (key, value) -> merged.putIfAbsent(key, value) }
        }
        return merged
    }

    private fun readFile(path: String): ByteArray? {
        val inode = lookup(path) ?: return null
        if (!inode.isRegular) return null
        return readInodeData(inode, MAX_PROP_BYTES)
    }

    private fun readInodeData(inode: Inode, maxBytes: Int): ByteArray? {
        if (inode.size <= 0L) return ByteArray(0)
        val size = inode.size.coerceAtMost(maxBytes.toLong()).toInt()
        if (size == 0) return ByteArray(0)
        val out = ByteArray(size)
        if (inode.hasInlineData) {
            val available = minOf(size, inode.block.size)
            inode.block.copyInto(out, 0, 0, available)
            return out
        }
        if (inode.hasExtents) {
            val blockSize = info?.blockSize ?: return null
            val runs = mutableListOf<Extent>()
            parseExtentNode(inode.block, runs, depth = 0)
            val maxBlockReads = size / blockSize + MAX_EXTENT_RUNS
            var blockReads = 0
            for (run in runs) {
                if (run.unwritten) continue
                var block = run.physicalBlock
                var logical = run.logicalBlock
                var blocksLeft = run.length
                while (blocksLeft > 0L) {
                    if (blockReads++ >= maxBlockReads) return out
                    val destination = logical * blockSize
                    if (destination >= size) break
                    val offset = destination.toInt()
                    val blockBytes = source.readExactly(block * blockSize, blockSize)
                    if (blockBytes != null) {
                        val copyLength = minOf(blockSize, size - offset, blockBytes.size)
                        blockBytes.copyInto(out, offset, 0, copyLength)
                    }
                    block++
                    logical++
                    blocksLeft--
                }
            }
            return out
        }
        val blockSize = info?.blockSize ?: return null
        var position = 0
        var logicalBlock = 0L
        while (position < size) {
            val physical = mapBlock(inode, logicalBlock)
            if (physical != 0L) {
                val blockBytes = source.readExactly(physical * blockSize, blockSize) ?: break
                val copyLength = minOf(blockSize, size - position, blockBytes.size)
                blockBytes.copyInto(out, position, 0, copyLength)
                position += copyLength
            } else {
                position += blockSize
            }
            logicalBlock++
        }
        return out
    }

    private fun lookup(path: String): Inode? {
        var inode = readInode(ROOT_INODE) ?: return null
        val segments = path.split('/').filter { it.isNotEmpty() }
        for (segment in segments) {
            if (!inode.isDirectory) return null
            val entries = readDirectory(inode) ?: return null
            val child = entries[segment] ?: return null
            inode = readInode(child) ?: return null
        }
        return inode
    }

    private fun readDirectory(inode: Inode): Map<String, Long>? {
        val data = readInodeData(inode, MAX_DIRECTORY_BYTES) ?: return null
        val entries = LinkedHashMap<String, Long>()
        var offset = 0
        while (offset + 8 <= data.size) {
            val child = le32(data, offset).toLong() and 0xFFFFFFFFL
            val recordLength = le16(data, offset + 4)
            val nameLength = data[offset + 6].toInt() and 0xFF
            if (child == 0L || recordLength < 8 || offset + recordLength > data.size) break
            if (nameLength in 1..(recordLength - 8)) {
                val name = String(data, offset + 8, nameLength, StandardCharsets.UTF_8)
                if (name != "." && name != "..") entries[name] = child
            }
            offset += recordLength
        }
        return entries
    }

    private fun mapBlock(inode: Inode, logicalBlock: Long): Long {
        val pointers = inode.block
        if (logicalBlock < DIRECT_BLOCKS) {
            return le32(pointers, logicalBlock.toInt() * 4).toLong() and 0xFFFFFFFFL
        }
        val perBlock = (info?.blockSize ?: return 0L) / 4
        var index = logicalBlock - DIRECT_BLOCKS
        val single = le32(pointers, DIRECT_BLOCKS * 4).toLong() and 0xFFFFFFFFL
        if (index < perBlock) {
            return readIndirect(single, index)
        }
        index -= perBlock
        val double = le32(pointers, (DIRECT_BLOCKS + 1) * 4).toLong() and 0xFFFFFFFFL
        if (index < perBlock.toLong() * perBlock) {
            val first = readIndirect(double, index / perBlock)
            return readIndirect(first, index % perBlock)
        }
        index -= perBlock.toLong() * perBlock
        val triple = le32(pointers, (DIRECT_BLOCKS + 2) * 4).toLong() and 0xFFFFFFFFL
        if (index < perBlock.toLong() * perBlock * perBlock) {
            val first = readIndirect(triple, index / (perBlock.toLong() * perBlock))
            val second = readIndirect(first, (index / perBlock) % perBlock)
            return readIndirect(second, index % perBlock)
        }
        return 0L
    }

    private fun readIndirect(block: Long, index: Long): Long {
        if (block == 0L || index < 0) return 0L
        val blockSize = info?.blockSize ?: return 0L
        val bytes = source.readExactly(block * blockSize, blockSize) ?: return 0L
        val offset = (index * 4).toInt()
        if (offset + 4 > bytes.size) return 0L
        return le32(bytes, offset).toLong() and 0xFFFFFFFFL
    }

    private fun parseExtentNode(node: ByteArray, output: MutableList<Extent>, depth: Int) {
        val blockSize = info?.blockSize ?: return
        if (depth > MAX_EXTENT_DEPTH) return
        if (extentNodes++ >= MAX_EXTENT_NODES) return
        if (le16(node, 0) != EXTENT_MAGIC) return
        val entries = le16(node, 2)
        val nodeDepth = le16(node, 6)
        if (entries < 0 || entries > MAX_EXTENT_ENTRIES) return
        if (nodeDepth == 0) {
            for (index in 0 until entries) {
                val offset = EXTENT_HEADER_SIZE + index * EXTENT_SIZE
                if (offset + EXTENT_SIZE > node.size) return
                val logical = le32(node, offset).toLong() and 0xFFFFFFFFL
                val rawLength = le16(node, offset + 4).toLong()
                val unwritten = rawLength > 32768L
                val length = if (unwritten) rawLength - 32768L else rawLength
                val high = le16(node, offset + 6).toLong() and 0xFFFFL
                val low = le32(node, offset + 8).toLong() and 0xFFFFFFFFL
                if (length <= 0L) continue
                if (output.size >= MAX_EXTENT_RUNS) return
                output += Extent(
                    logicalBlock = logical,
                    length = length,
                    physicalBlock = (high shl 32) or low,
                    unwritten = unwritten,
                )
            }
            return
        }
        val indexSize = EXTENT_SIZE
        for (index in 0 until entries) {
            val offset = EXTENT_HEADER_SIZE + index * indexSize
            if (offset + indexSize > node.size) return
            val childLow = le32(node, offset + 4).toLong() and 0xFFFFFFFFL
            val childHigh = le16(node, offset + 8).toLong() and 0xFFFFL
            val childBlock = (childHigh shl 32) or childLow
            if (childBlock == 0L) continue
            val child = source.readExactly(childBlock * blockSize, blockSize) ?: continue
            parseExtentNode(child, output, depth + 1)
        }
    }

    private fun readInode(number: Long): Inode? {
        if (number <= 0L || inodesPerGroup <= 0) return null
        val index = (number - 1).toInt()
        val group = index / inodesPerGroup
        val inGroup = index % inodesPerGroup
        val descriptorOffset = (firstDataBlock + 1) * (info?.blockSize ?: return null) +
            group.toLong() * descriptorSize
        val descriptor = source.readExactly(descriptorOffset, maxOf(descriptorSize, 64)) ?: return null
        if (descriptor.size < 32) return null
        var inodeTable = le32(descriptor, 8).toLong() and 0xFFFFFFFFL
        if (is64Bit && descriptor.size >= 44) {
            inodeTable = inodeTable or
                ((le32(descriptor, 40).toLong() and 0xFFFFFFFFL) shl 32)
        }
        if (inodeTable == 0L) return null
        val inodeOffset = inodeTable * (info?.blockSize ?: return null) +
            inGroup.toLong() * inodeSize
        val bytes = source.readExactly(inodeOffset, maxOf(inodeSize, INODE_FIXED_SIZE)) ?: return null
        if (bytes.size < INODE_FIXED_SIZE) return null
        val mode = le16(bytes, 0)
        var size = le32(bytes, 4).toLong() and 0xFFFFFFFFL
        if (bytes.size >= 112) {
            val high = le32(bytes, 108).toLong() and 0xFFFFFFFFL
            if (high != 0L) size = size or (high shl 32)
        }
        val flags = le32(bytes, 32).toLong() and 0xFFFFFFFFL
        val block = bytes.copyOfRange(40, 100)
        return Inode(number = number, mode = mode, size = size, flags = flags, block = block)
    }

    private fun parseProps(bytes: ByteArray): Map<String, String> {
        val props = LinkedHashMap<String, String>()
        String(bytes, StandardCharsets.UTF_8).lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val separator = line.indexOf('=')
            if (separator <= 0) return@forEach
            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) props.putIfAbsent(key, value)
        }
        return props
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }

    companion object {
        private const val SUPERBLOCK_OFFSET = 1024L
        private const val SUPERBLOCK_SIZE = 1024
        private const val EXT4_MAGIC = 0xEF53
        private const val OFFSET_INODES_COUNT = 0
        private const val OFFSET_BLOCKS_COUNT_LO = 4
        private const val OFFSET_FIRST_DATA_BLOCK = 20
        private const val OFFSET_LOG_BLOCK_SIZE = 24
        private const val OFFSET_BLOCKS_PER_GROUP = 32
        private const val OFFSET_INODES_PER_GROUP = 40
        private const val OFFSET_MAGIC = 56
        private const val OFFSET_INODE_SIZE = 88
        private const val OFFSET_FEATURE_INCOMPAT = 96
        private const val OFFSET_FEATURE_RO_COMPAT = 100
        private const val OFFSET_UUID = 104
        private const val OFFSET_VOLUME_NAME = 120
        private const val OFFSET_BLOCKS_COUNT_HI = 0x150
        private const val OFFSET_DESC_SIZE = 254
        private const val ROOT_INODE = 2L
        private const val INODE_FIXED_SIZE = 128
        private const val DIRECT_BLOCKS = 12
        private const val EXTENT_MAGIC = 0xF30A
        private const val EXTENT_HEADER_SIZE = 12
        private const val EXTENT_SIZE = 12
        private const val MAX_EXTENT_ENTRIES = 340
        private const val MAX_EXTENT_DEPTH = 5
        private const val MAX_EXTENT_RUNS = 8192
        private const val MAX_EXTENT_NODES = 16384
        private const val MAX_PROP_BYTES = 2 * 1024 * 1024
        private const val MAX_DIRECTORY_BYTES = 16 * 1024 * 1024
        private val PROP_PATHS = listOf(
            "system/build.prop",
            "build.prop",
            "system/etc/build.prop",
            "etc/build.prop",
            "prop.default",
            "system/etc/prop.default",
            "system_ext/etc/build.prop",
            "product/etc/build.prop",
            "vendor/build.prop",
        )

        internal fun le16(bytes: ByteArray, offset: Int): Int {
            if (offset < 0 || offset + 2 > bytes.size) return 0
            return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
        }

        internal fun le32(bytes: ByteArray, offset: Int): Int {
            if (offset < 0 || offset + 4 > bytes.size) return 0
            return (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
        }
    }
}
