package com.dsu.extended.compat

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import org.tukaani.xz.XZInputStream

/**
 * Reads enough of a GSI to make a useful compatibility decision: container,
 * filesystem, sparse format, size, partition layout and build.prop values.
 * Nothing is mounted or executed; the image is only read.
 */
class GsiImageAnalyzer {

    fun analyze(
        input: GsiImageInput,
        isCancelled: () -> Boolean = { false },
    ): GsiFacts {
        val header = runCatching { input.readHeader() }.getOrDefault(ByteArray(0))
        return try {
            checkCancelled(isCancelled)
            when (detectContainer(header)) {
                ImageContainer.GZIP -> analyzeCompressed(input, ImageContainer.GZIP, isCancelled) {
                    GZIPInputStream(BufferedInputStream(it, BUFFER_BYTES))
                }

                ImageContainer.XZ -> analyzeCompressed(input, ImageContainer.XZ, isCancelled) {
                    XZInputStream(BufferedInputStream(it, BUFFER_BYTES))
                }

                ImageContainer.ZIP -> analyzeZip(input, isCancelled)
                ImageContainer.RAW -> analyzeRaw(input, header, isCancelled)
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            GsiFacts(
                container = detectContainer(header),
                storedSizeBytes = input.storedSize,
                declaredRawSizeBytes = input.declaredRawSize,
                error = t.message ?: t.javaClass.simpleName,
            )
        }
    }

    private fun analyzeCompressed(
        input: GsiImageInput,
        container: ImageContainer,
        isCancelled: () -> Boolean,
        streamFactory: (InputStream) -> InputStream,
    ): GsiFacts {
        val raw = input.openStream()
        val decompressed = try {
            streamFactory(raw)
        } catch (t: Throwable) {
            runCatching { raw.close() }
            throw t
        }
        return decompressed.use {
            analyzeLogicalStream(
                stream = it,
                container = container,
                input = input,
                expectedRawSize = input.declaredRawSize,
                isCancelled = isCancelled,
            )
        }
    }

    private fun analyzeRaw(
        input: GsiImageInput,
        header: ByteArray,
        isCancelled: () -> Boolean,
    ): GsiFacts {
        val base = baseFacts(header, ImageContainer.RAW, input)
        if (base.hasGpt || base.hasSuper || base.hasBootMagic) return base

        if (!base.sparse && base.fileSystem == ImageFileSystem.EXT4) {
            val seekable = input.seekableSource
            if (seekable != null) {
                val ext4 = Ext4Image(seekable)
                val info = runCatching { ext4.open() }.getOrNull()
                if (info != null) {
                    val props = runCatching { ext4.readBuildProps() }.getOrDefault(emptyMap())
                    if (props.isNotEmpty()) {
                        return base.copy(
                            fileSystem = ImageFileSystem.EXT4,
                            rawSizeBytes = info.rawSize,
                            props = props,
                            propsSource = "ext4",
                        )
                    }
                }
            }
        }

        // Sparse images, EROFS and ext4 layouts the parser cannot follow are read
        // sequentially. This still yields the filesystem, size and build.prop text.
        return analyzeLogicalStream(
            stream = input.openStream(),
            container = ImageContainer.RAW,
            input = input,
            expectedRawSize = input.declaredRawSize,
            isCancelled = isCancelled,
        )
    }

    private fun analyzeLogicalStream(
        stream: InputStream,
        container: ImageContainer,
        input: GsiImageInput,
        expectedRawSize: Long,
        isCancelled: () -> Boolean,
    ): GsiFacts {
        val buffered = BufferedInputStream(stream, BUFFER_BYTES)
        val logical = try {
            SparseStream(buffered)
        } catch (t: Throwable) {
            runCatching { buffered.close() }
            throw t
        }
        logical.use {
            if (logical.unsparseSize > MAX_DECODED_BYTES) {
                throw IOException("Decoded image exceeds the safety limit")
            }
            val first = ByteArray(GsiImageInput.HEADER_BYTES)
            var firstLength = 0
            while (firstLength < first.size) {
                val read = logical.read(first, firstLength, first.size - firstLength)
                if (read < 0) break
                firstLength += read
            }
            val facts = baseFacts(first.copyOf(firstLength), container, input).copy(
                sparse = logical.isSparse,
            )
            val scanner = PropLineScanner()
            scanner.accept(first, 0, firstLength)
            var totalRead = firstLength.toLong()
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                checkCancelled(isCancelled)
                val read = logical.read(buffer, 0, buffer.size)
                if (read < 0) break
                totalRead += read
                if (totalRead > MAX_DECODED_BYTES) {
                    throw IOException("Decoded image exceeds the safety limit")
                }
                scanner.accept(buffer, 0, read)
            }
            val unsparseSize = logical.unsparseSize.takeIf { it > 0L }
            val rawSize = unsparseSize
                ?: totalRead.takeIf { it > 0L }
                ?: expectedRawSize
            val mismatch = expectedRawSize > 0L &&
                unsparseSize == null &&
                kotlin.math.abs(expectedRawSize - totalRead) > SIZE_MISMATCH_TOLERANCE
            return facts.copy(
                sparse = logical.isSparse,
                rawSizeBytes = rawSize,
                props = scanner.props,
                propsSource = "stream",
                scanComplete = true,
                sizeMismatch = mismatch,
            )
        }
    }

    private fun analyzeZip(input: GsiImageInput, isCancelled: () -> Boolean): GsiFacts {
        val images = mutableListOf<String>()
        var selectedEntry: String? = null
        var systemEntry: String? = null

        ZipInputStream(BufferedInputStream(input.openStream(), BUFFER_BYTES)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                checkCancelled(isCancelled)
                if (!entry.isDirectory && entry.name.endsWith(".img", ignoreCase = true)) {
                    val name = entry.name
                    images += name
                    val baseName = name.substringAfterLast('/')
                    val partition = baseName.dropLast(IMAGE_SUFFIX_LENGTH)
                    if (selectedEntry == null) selectedEntry = name
                    if (systemEntry == null && partition.equals("system", ignoreCase = true)) {
                        systemEntry = name
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val selected = systemEntry ?: selectedEntry
            ?: return GsiFacts(
                container = ImageContainer.ZIP,
                storedSizeBytes = input.storedSize,
                declaredRawSizeBytes = input.declaredRawSize,
                zipImages = images,
                propsSource = null,
            )

        ZipInputStream(BufferedInputStream(input.openStream(), BUFFER_BYTES)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                checkCancelled(isCancelled)
                if (entry.name == selected) {
                    val facts = analyzeLogicalStream(
                        stream = zip,
                        container = ImageContainer.ZIP,
                        input = input,
                        expectedRawSize = entry.size.takeIf { it > 0L } ?: input.declaredRawSize,
                        isCancelled = isCancelled,
                    )
                    return facts.copy(zipImages = images)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return GsiFacts(
            container = ImageContainer.ZIP,
            storedSizeBytes = input.storedSize,
            declaredRawSizeBytes = input.declaredRawSize,
            zipImages = images,
        )
    }

    private fun checkCancelled(isCancelled: () -> Boolean) {
        if (isCancelled()) throw CancellationException("GSI analysis cancelled")
    }

    private fun baseFacts(
        header: ByteArray,
        container: ImageContainer,
        input: GsiImageInput,
    ): GsiFacts {
        val sparse = header.size >= 4 && le32(header, 0) == SPARSE_MAGIC
        val rawSize = if (sparse && header.size >= 28) {
            val blockSize = le32(header, 12).toLong() and 0xFFFFFFFFL
            val blocks = le32(header, 16).toLong() and 0xFFFFFFFFL
            blockSize * blocks
        } else {
            input.declaredRawSize
        }
        return GsiFacts(
            container = container,
            fileSystem = detectFileSystem(header),
            sparse = sparse,
            rawSizeBytes = rawSize,
            storedSizeBytes = input.storedSize,
            declaredRawSizeBytes = input.declaredRawSize,
            hasGpt = hasGpt(header),
            hasSuper = hasSuper(header),
            hasBootMagic = ascii(header, 0, 8) == "ANDROID!",
        )
    }

    private fun detectContainer(header: ByteArray): ImageContainer {
        if (header.size >= 2 && header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) {
            return ImageContainer.GZIP
        }
        if (header.size >= 6 &&
            header[0] == 0xFD.toByte() &&
            header[1] == 0x37.toByte() &&
            header[2] == 0x7A.toByte() &&
            header[3] == 0x58.toByte() &&
            header[4] == 0x5A.toByte() &&
            header[5] == 0x00.toByte()
        ) {
            return ImageContainer.XZ
        }
        if (header.size >= 4 &&
            header[0] == 'P'.code.toByte() &&
            header[1] == 'K'.code.toByte() &&
            ((header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) ||
                (header[2] == 0x05.toByte() && header[3] == 0x06.toByte()) ||
                (header[2] == 0x07.toByte() && header[3] == 0x08.toByte()))
        ) {
            return ImageContainer.ZIP
        }
        return ImageContainer.RAW
    }

    private fun detectFileSystem(header: ByteArray): ImageFileSystem {
        if (header.size >= 1082 && le16(header, 1080) == EXT4_MAGIC) {
            return ImageFileSystem.EXT4
        }
        if (header.size >= 1028) {
            when (le32(header, 1024)) {
                EROFS_MAGIC -> return ImageFileSystem.EROFS
                F2FS_MAGIC -> return ImageFileSystem.F2FS
            }
        }
        return ImageFileSystem.UNKNOWN
    }

    private fun hasGpt(header: ByteArray): Boolean {
        if (ascii(header, 512, 8) == "EFI PART") return true
        if (header.size < 512) return false
        val bootSignature = le16(header, 510)
        val protectiveType = header[450].toInt() and 0xFF
        return bootSignature == 0xAA55 && protectiveType == 0xEE
    }

    private fun hasSuper(header: ByteArray): Boolean {
        return (header.size >= 4 && le32(header, 0) == SUPER_MAGIC) ||
            (header.size >= 4100 && le32(header, 4096) == SUPER_MAGIC)
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String {
        if (offset < 0 || length <= 0 || offset + length > bytes.size) return ""
        return String(bytes, offset, length, Charsets.US_ASCII)
    }

    private class PropLineScanner {
        private val fragment = ByteArrayOutputStream(MAX_LINE_BYTES)
        private val result = LinkedHashMap<String, String>()

        val props: Map<String, String> get() = result

        fun accept(bytes: ByteArray, offset: Int, length: Int) {
            var index = offset
            val end = offset + length
            while (index < end) {
                val byte = bytes[index++].toInt() and 0xFF
                if (byte == 0x0A) {
                    processFragment(fragment.toByteArray(), complete = true)
                    fragment.reset()
                    continue
                }
                if (fragment.size() >= MAX_LINE_BYTES) {
                    val data = fragment.toByteArray()
                    processFragment(data, complete = false)
                    fragment.reset()
                    val keep = minOf(data.size, RETAIN_BYTES)
                    fragment.write(data, data.size - keep, keep)
                }
                fragment.write(byte)
            }
        }

        private fun processFragment(bytes: ByteArray, complete: Boolean) {
            if (bytes.isEmpty()) return
            val text = String(bytes, Charsets.UTF_8)
            for (key in WANTED_PROPS) {
                var searchFrom = 0
                while (searchFrom < text.length) {
                    val keyIndex = text.indexOf("$key=", searchFrom)
                    if (keyIndex < 0) break
                    val valueStart = keyIndex + key.length + 1
                    val newline = text.indexOf('\n', valueStart)
                    if (newline < 0 && !complete) {
                        searchFrom = keyIndex + 1
                        continue
                    }
                    val valueEnd = if (newline < 0) text.length else newline
                    val value = text.substring(valueStart, valueEnd).trim()
                    if (value.isNotEmpty() && isValid(key, value)) {
                        result.putIfAbsent(key, value)
                    }
                    searchFrom = valueEnd + 1
                }
            }
        }

        private fun isValid(key: String, value: String): Boolean {
            if (value.isEmpty() || value.length > 512 || value.any { it.isISOControl() }) return false
            return when (key) {
                "ro.build.version.sdk", "ro.system.build.version.sdk" ->
                    value.toIntOrNull()?.let { it in 18..100 } ?: false

                "ro.build.version.release",
                "ro.system.build.version.release",
                "ro.build.version.release_or_codename",
                -> RELEASE_PATTERN.matches(value)

                "ro.treble.enabled" -> value == "true" || value == "false"
                "ro.product.cpu.abi" -> value in KNOWN_ABIS
                "ro.product.cpu.abilist" -> value.split(',').all { it.trim() in KNOWN_ABIS }
                "ro.vndk.version" -> value == "None" || VERSION_PATTERN.matches(value)
                "ro.vndk.lite" -> value == "true" || value == "false"
                "ro.build.type", "ro.system.build.type" -> value in BUILD_TYPES
                "ro.build.fingerprint", "ro.system.build.fingerprint" ->
                    value.contains(':') && value.contains('/')
                "ro.product.device", "ro.product.name" -> NAME_PATTERN.matches(value)
                "ro.build.version.security_patch" -> PATCH_PATTERN.matches(value)
                else -> true
            }
        }

        companion object {
            private const val MAX_LINE_BYTES = 16 * 1024
            private const val RETAIN_BYTES = 4 * 1024
            private val RELEASE_PATTERN = Regex("""[A-Za-z0-9]+(\.[A-Za-z0-9]+)*""")
            private val VERSION_PATTERN = Regex("""[0-9]+(\.[0-9]+)*""")
            private val PATCH_PATTERN = Regex("""[0-9]{4}-[0-9]{2}-[0-9]{2}""")
            private val NAME_PATTERN = Regex("""[A-Za-z0-9._-]{1,64}""")
            private val BUILD_TYPES = setOf("user", "userdebug", "eng")
            private val KNOWN_ABIS = setOf(
                "armeabi",
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64",
                "mips",
                "mips64",
                "riscv64",
            )
            private val WANTED_PROPS = setOf(
                "ro.build.version.sdk",
                "ro.system.build.version.sdk",
                "ro.build.version.release",
                "ro.system.build.version.release",
                "ro.build.version.release_or_codename",
                "ro.product.cpu.abi",
                "ro.product.cpu.abilist",
                "ro.vndk.version",
                "ro.vndk.lite",
                "ro.treble.enabled",
                "ro.build.fingerprint",
                "ro.system.build.fingerprint",
                "ro.build.type",
                "ro.system.build.type",
                "ro.product.device",
                "ro.product.name",
                "ro.build.version.security_patch",
            )
        }
    }

    companion object {
        private const val BUFFER_BYTES = 64 * 1024
        private const val SIZE_MISMATCH_TOLERANCE = 1024L * 1024L
        private const val MAX_DECODED_BYTES = 64L * 1024L * 1024L * 1024L
        private const val IMAGE_SUFFIX_LENGTH = 4
        private const val SPARSE_MAGIC = 0xED26FF3A.toInt()
        private const val EXT4_MAGIC = 0xEF53
        private const val EROFS_MAGIC = 0xE0F5E1E2.toInt()
        private const val F2FS_MAGIC = 0xF2F52010.toInt()
        private const val SUPER_MAGIC = 0x616C4467

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
