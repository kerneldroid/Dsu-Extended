package com.dsu.extended.compat

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SparseStreamTest {

    @Test
    fun decodesRawFillAndDontCareChunks() {
        val sparse = buildSparse()
        SparseStream(ByteArrayInputStream(sparse)).use { stream ->
            assertTrue(stream.isSparse)
            assertEquals(16L, stream.unsparseSize)
            val output = stream.readBytes()
            assertArrayEquals(
                "ABCD".toByteArray() +
                    "WXYZ".toByteArray() +
                    ByteArray(8),
                output,
            )
        }
    }

    @Test
    fun passesThroughNonSparseInput() {
        val data = "ordinary image bytes".toByteArray()
        SparseStream(ByteArrayInputStream(data)).use { stream ->
            assertFalse(stream.isSparse)
            assertEquals(-1L, stream.unsparseSize)
            assertArrayEquals(data, stream.readBytes())
        }
    }

    @Test
    fun rejectsBrokenChunk() {
        val broken = buildSparse().copyOf(28)
        val stream = SparseStream(ByteArrayInputStream(broken))
        assertEquals(16L, stream.unsparseSize)
        runCatching { stream.readBytes() }.onSuccess {
            throw AssertionError("Expected a truncated chunk failure")
        }
        stream.close()
    }

    private fun buildSparse(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(le32(0xED26FF3A.toInt()))
        out.write(le16(1))
        out.write(le16(0))
        out.write(le16(28))
        out.write(le16(12))
        out.write(le32(4))
        out.write(le32(4))
        out.write(le32(3))
        out.write(le32(0))

        out.write(le16(0xCAC1))
        out.write(le16(0))
        out.write(le32(1))
        out.write(le32(16))
        out.write("ABCD".toByteArray())

        out.write(le16(0xCAC2))
        out.write(le16(0))
        out.write(le32(1))
        out.write(le32(16))
        out.write("WXYZ".toByteArray())

        out.write(le16(0xCAC3))
        out.write(le16(0))
        out.write(le32(2))
        out.write(le32(12))
        return out.toByteArray()
    }

    private fun le16(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
        )
    }

    private fun le32(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
        )
    }
}
