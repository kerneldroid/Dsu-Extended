package com.dsu.extended.compat

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class Ext4ImageTest {

    private val buildProp = """
        ro.build.version.sdk=34
        ro.build.version.release=14
        ro.product.cpu.abi=arm64-v8a
        ro.product.cpu.abilist=arm64-v8a,armeabi-v7a
        ro.vndk.version=34
        ro.treble.enabled=true
        ro.build.type=user
        ro.build.fingerprint=test/device/arm64:14/UQ1A/1:user/release-keys
        ro.build.version.security_patch=2026-09-01
    """.trimIndent()

    @Test
    fun readsBuildPropFromExt4ImageWithFourKilobyteBlocks() {
        val image = createExt4Image(blockSize = 4096)
        val props = withSource(image) { source ->
            val ext4 = Ext4Image(source)
            val info = ext4.open()
            assertTrue(info != null)
            assertTrue(info!!.rawSize > 0L)
            ext4.readBuildProps()
        }
        assertEquals("34", props["ro.build.version.sdk"])
        assertEquals("arm64-v8a", props["ro.product.cpu.abi"])
        assertEquals("user", props["ro.build.type"])
    }

    @Test
    fun readsBuildPropFromExt4ImageWithOneKilobyteBlocks() {
        val image = createExt4Image(blockSize = 1024)
        val props = withSource(image) { source ->
            Ext4Image(source).apply { open() }.readBuildProps()
        }
        assertEquals("34", props["ro.build.version.sdk"])
        assertEquals("arm64-v8a", props["ro.product.cpu.abi"])
    }

    @Test
    fun analyzerReadsExt4AndGzipImages() {
        val image = createExt4Image(blockSize = 4096)
        val ext4Facts = analyze(image, gzip = false)
        assertEquals(ImageFileSystem.EXT4, ext4Facts.fileSystem)
        assertEquals(34, ext4Facts.sdk)
        assertEquals("arm64-v8a", ext4Facts.abi)
        assertEquals("ext4", ext4Facts.propsSource)

        val gzip = File(image.parentFile, "system.img.gz")
        FileInputStream(image).use { input ->
            GZIPOutputStream(FileOutputStream(gzip)).use { output -> input.copyTo(output) }
        }
        val gzipFacts = analyze(gzip, gzip = true)
        assertEquals(ImageContainer.GZIP, gzipFacts.container)
        assertEquals(ImageFileSystem.EXT4, gzipFacts.fileSystem)
        assertEquals(34, gzipFacts.sdk)
        assertEquals("arm64-v8a", gzipFacts.abi)
    }

    private fun analyze(file: File, gzip: Boolean): GsiFacts {
        return withSource(file) { source ->
            GsiImageAnalyzer().analyze(
                GsiImageInput(
                    displayName = file.name,
                    storedSize = file.length(),
                    declaredRawSize = if (gzip) {
                        File(file.absolutePath.removeSuffix(".gz")).length()
                    } else {
                        file.length()
                    },
                    seekable = source,
                    streamFactory = { FileInputStream(file) },
                ),
            )
        }
    }

    private fun <T> withSource(file: File, block: (SeekableSource) -> T): T {
        val channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)
        val source = ChannelSeekableSource(channel, channel)
        return try {
            block(source)
        } finally {
            source.close()
        }
    }

    private fun createExt4Image(blockSize: Int): File {
        assumeTrue("mke2fs is required for this test", commandExists("mke2fs"))
        val directory = createTempDir("dsu-ext4")
        val root = File(directory, "root")
        root.mkdirs()
        File(root, "build.prop").writeText(buildProp)
        val image = File(directory, "system.img")
        val blocks = if (blockSize == 1024) 8192 else 2048
        val process = ProcessBuilder(
            "mke2fs",
            "-F",
            "-q",
            "-t",
            "ext4",
            "-b",
            blockSize.toString(),
            "-d",
            root.absolutePath,
            image.absolutePath,
            blocks.toString(),
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        assertTrue("mke2fs failed: $output", exit == 0)
        assertTrue(image.exists())
        return image
    }

    private fun createTempDir(prefix: String): File {
        val directory = kotlin.io.path.createTempDirectory(prefix).toFile()
        directory.deleteOnExit()
        return directory
    }

    private fun commandExists(command: String): Boolean {
        return runCatching {
            val process = ProcessBuilder("sh", "-c", "command -v $command").start()
            process.waitFor() == 0
        }.getOrDefault(false)
    }
}
