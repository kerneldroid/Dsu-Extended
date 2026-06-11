package com.dsu.extended.preparation

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Job
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import com.dsu.extended.core.StorageManager

class FileUnPacker(
    private val storageManager: StorageManager,
    private val inputFile: Uri,
    private val outputFile: String,
    private val installationJob: Job,
    private val onProgressChange: (Float) -> Unit,
) {

    private val inputFileSize = storageManager.getFilesizeFromUri(inputFile)

    private fun copy(
        inputStr: InputStream,
        outputStr: OutputStream,
        onReadedBuffer: (Long) -> Unit,
    ) {
        inputStr.use { input ->
            outputStr.use { output ->
                val buffer = ByteArray(8 * 1024)
                var n: Int
                var readed: Long = 0
                while (-1 != input.read(buffer).also { n = it } && !installationJob.isCancelled) {
                    readed += n.toLong()
                    onReadedBuffer(readed)
                    output.write(buffer, 0, n)
                }
                output.flush()
            }
        }
    }

    fun pack(): Pair<Uri, Long> {
        val finalFile = storageManager.createDocumentFile(outputFile)
        val inputStream = storageManager.openInputStream(inputFile)
        val outputStream = storageManager.openOutputStream(finalFile.uri)

        copy(inputStream, GzipCompressorOutputStream(outputStream)) {
            updateProgress(inputFileSize, it)
        }
        val fileLength = storageManager.getFilesizeFromUri(finalFile.uri)
        return Pair(finalFile.uri, fileLength)
    }

    fun unpack(): Pair<Uri, Long> {
        val finalFile = storageManager.createDocumentFile(outputFile)
        val inputStream = storageManager.openInputStream(inputFile)
        val outputStream = storageManager.openOutputStream(finalFile.uri)

        val archiveInputStream =
            with(storageManager.getFilenameFromUri(inputFile)) {
                when {
                    endsWith("xz") -> XZCompressorInputStream(inputStream)
                    endsWith("gz") -> GzipCompressorInputStream(inputStream)
                    endsWith("gzip") -> GzipCompressorInputStream(inputStream)
                    else -> throw Exception("File type not supported")
                }
            }
        copy(archiveInputStream, outputStream) {
            updateProgress(inputFileSize, (archiveInputStream as? GzipCompressorInputStream)?.compressedCount
                ?: (archiveInputStream as? XZCompressorInputStream)?.compressedCount ?: it)
        }
        val fileLength = storageManager.getFilesizeFromUri(finalFile.uri)
        return Pair(finalFile.uri, fileLength)
    }

    private fun updateProgress(fileSize: Long, readed: Long) {
        val percent: Float = readed.toFloat() / fileSize.toFloat()
        onProgressChange(percent)
    }
}
