package com.dsu.extended.installer.root

import android.app.Application
import android.gsi.IGsiService
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SharedMemory
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.lsposed.hiddenapibypass.HiddenApiBypass
import com.dsu.extended.model.DSUInstallationSource
import com.dsu.extended.model.ImagePartition
import com.dsu.extended.model.Type
import com.dsu.extended.preparation.InstallationStep
import com.dsu.extended.service.PrivilegedProvider
import com.dsu.extended.util.AppLogger

/**
 * DSU Installer implementation using Android APIs
 * Based on InstallationAsyncTask from DynamicSystemInstallationService
 * DynamicSystemInstallationService/src/com/android/dynsystem/InstallationAsyncTask.java
 *
 * Calling APIs directly to install images are fast, because we can apply images directly
 * instead of preparing a file exclusively to install via DSU system-app
 * also, having access to APIs make everything more flexible.
 *
 * Unfortunately, this implementation has a downside, it requires "MANAGE_DYNAMIC_SYSTEM"
 * and this permission has a protection level of "signature".
 *
 * That's why this installation way requires root.
 */
class DSUInstaller(
    private val application: Application,
    private val userdataSize: Long,
    private val dsuInstallation: DSUInstallationSource,
    private var installationJob: Job = Job(),
    private val onInstallationError: (error: InstallationStep, errorInfo: String) -> Unit,
    private val onInstallationProgressUpdate: (progress: Float, partition: String) -> Unit,
    private val onCreatePartition: (partition: String) -> Unit,
    private val onInstallationStepUpdate: (step: InstallationStep) -> Unit,
    private val onInstallationSuccess: () -> Unit,
) : () -> Unit, DynamicSystemImpl() {

    private val tag = this.javaClass.simpleName

    object Constants {
        const val DEFAULT_SLOT = "dsu"
        const val SHARED_MEM_SIZE: Int = 524288
        const val MIN_PROGRESS_TO_PUBLISH = (1 shl 27).toLong()
    }

    private class MappedMemoryBuffer(var mBuffer: ByteBuffer?) :
        AutoCloseable {
        override fun close() {
            if (mBuffer != null) {
                SharedMemory.unmap(mBuffer!!)
                mBuffer = null
            }
        }
    }

    private val UNSUPPORTED_PARTITIONS: List<String> = listOf(
        "vbmeta",
        "boot",
        "userdata",
        "dtbo",
        "super_empty",
        "system_other",
        "scratch",
    )

    private fun isPartitionSupported(partitionName: String): Boolean =
        !UNSUPPORTED_PARTITIONS.contains(partitionName)

    private fun getFdDup(sharedMemory: SharedMemory): ParcelFileDescriptor {
        return HiddenApiBypass.invoke(
            sharedMemory.javaClass,
            sharedMemory,
            "getFdDup",
        ) as ParcelFileDescriptor
    }

    private fun shouldInstallEntry(name: String): Boolean {
        if (!name.endsWith(".img")) {
            return false
        }
        val partitionName = extractPartitionName(name)
        return isPartitionSupported(partitionName)
    }

    private fun extractPartitionName(entryName: String): String {
        return entryName
            .substringAfterLast("/")
            .removeSuffix(".img")
    }

    private fun publishProgress(bytesRead: Long, totalBytes: Long, partition: String) {
        val safeBytesRead = bytesRead.coerceAtLeast(0L)
        val fallbackTotal = runCatching { installationProgress.total_bytes }.getOrDefault(0L)
        val safeTotal =
            when {
                totalBytes > 0L -> totalBytes
                fallbackTotal > 0L -> fallbackTotal
                else -> 0L
            }
        val progress =
            if (safeTotal > 0L && safeBytesRead > 0L) {
                (safeBytesRead.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        onInstallationProgressUpdate(progress, partition)
    }

    private fun installWritablePartition(
        partition: String,
        partitionSize: Long,
        readOnly: Boolean = false,
    ) {
        onCreatePartition(partition)
        val job = Job()
        CoroutineScope(Dispatchers.IO + job).launch {
            if (!createNewPartition(partition, partitionSize, readOnly)) {
                job.cancel()
                return@launch
            }
            job.complete()
        }
        publishProgress(0L, partitionSize, partition)
        var prevInstalledSize = 0L
        while (job.isActive) {
            if (installationJob.isCancelled) {
                job.cancel()
                return
            }
            val installedSize = installationProgress.bytes_processed
            if (installedSize > prevInstalledSize + Constants.MIN_PROGRESS_TO_PUBLISH) {
                prevInstalledSize = installedSize
                publishProgress(installedSize, partitionSize, partition)
            }
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                return
            }
        }
        if (!closePartition()) {
            AppLogger.e(tag, "Failed to close partition after write", null, "partition" to partition)
            onInstallationError(InstallationStep.ERROR_CREATE_PARTITION, partition)
            return
        }

        // Ensure a 100% mark is published.
        if (prevInstalledSize != partitionSize) {
            publishProgress(partitionSize, partitionSize, partition)
        }
        AppLogger.i(
            tag,
            "Partition installed",
            "partition" to partition,
            "readOnly" to readOnly,
            "partitionSize" to partitionSize,
        )
    }

    private fun installImage(
        partition: String,
        uncompressedSize: Long,
        inputStream: InputStream,
        readOnly: Boolean = true,
    ) {
        val sis = SparseInputStream(
            BufferedInputStream(inputStream),
        )
        val partitionSize = if (sis.unsparseSize != -1L) sis.unsparseSize else uncompressedSize
        onCreatePartition(partition)
        if (!createNewPartition(partition, partitionSize, readOnly)) {
            return
        }
        onInstallationStepUpdate(InstallationStep.INSTALLING_ROOTED)
        SharedMemory.create("dsu_buffer_$partition", Constants.SHARED_MEM_SIZE)
            .use { sharedMemory ->
                MappedMemoryBuffer(sharedMemory.mapReadWrite()).use { mappedBuffer ->
                    val fdDup = getFdDup(sharedMemory)
                    if (!setAshmem(fdDup, sharedMemory.size.toLong())) {
                        onInstallationError(InstallationStep.ERROR, "Failed to map installation ashmem")
                        installationJob.cancel()
                        return
                    }
                    publishProgress(0L, partitionSize, partition)
                    var installedSize: Long = 0
                    val readBuffer = ByteArray(sharedMemory.size)
                    val buffer = mappedBuffer.mBuffer
                    var numBytesRead: Int
                    while (0 < sis.read(readBuffer, 0, readBuffer.size)
                            .also { numBytesRead = it }
                    ) {
                        if (installationJob.isCancelled) {
                            return
                        }
                        buffer!!.position(0)
                        buffer.put(readBuffer, 0, numBytesRead)
                        if (!submitFromAshmem(numBytesRead.toLong())) {
                            onInstallationError(InstallationStep.ERROR, "Failed to submit partition chunk")
                            installationJob.cancel()
                            return
                        }
                        installedSize += numBytesRead.toLong()
                        publishProgress(installedSize, partitionSize, partition)
                    }
                    publishProgress(partitionSize, partitionSize, partition)
                }
            }

        if (!closePartition()) {
            AppLogger.e(tag, "Failed to close partition after image write", null, "partition" to partition)
            onInstallationError(InstallationStep.ERROR_CREATE_PARTITION, partition)
            return
        }
        AppLogger.i(
            tag,
            "Partition installed",
            "partition" to partition,
            "readOnly" to readOnly,
            "partitionSize" to partitionSize,
        )
    }

    private fun installStreamingZipUpdate(inputStream: InputStream): Boolean {
        val zis = ZipInputStream(inputStream)
        while (true) {
            val entry = zis.nextEntry ?: break
            val fileName = entry.name
            if (shouldInstallEntry(fileName)) {
                installImageFromAnEntry(entry, zis)
            } else {
                AppLogger.d(tag, "Entry skipped", "fileName" to fileName)
            }
            if (installationJob.isCancelled) {
                break
            }
        }
        return true
    }

    private fun installImageFromAnEntry(entry: ZipEntry, inputStream: InputStream) {
        val fileName = entry.name
        AppLogger.i(tag, "Installing zip entry", "fileName" to fileName, "size" to entry.size)
        val partitionName = extractPartitionName(fileName)
        val uncompressedSize = entry.size
        installImage(partitionName, uncompressedSize, inputStream)
    }

    private fun startInstallation() {
        PrivilegedProvider.getService().setDynProp()
        if (isInUse) {
            onInstallationError(InstallationStep.ERROR_ALREADY_RUNNING_DYN_OS, "")
            return
        }
        if (isInstalled) {
            onInstallationError(InstallationStep.ERROR_REQUIRES_DISCARD_DSU, "")
            return
        }
        forceStopDSU()
        if (!startInstallation(Constants.DEFAULT_SLOT)) {
            onInstallationError(InstallationStep.ERROR, "Failed to start DSU installation session")
            return
        }
        installWritablePartition("userdata", userdataSize)
        if (installationJob.isCancelled) {
            return
        }
        when (dsuInstallation.type) {
            Type.SINGLE_SYSTEM_IMAGE -> {
                installImage(
                    "system",
                    dsuInstallation.fileSize,
                    dsuInstallation.uri,
                )
            }

            Type.MULTIPLE_IMAGES -> {
                installImages(dsuInstallation.images)
            }

            Type.DSU_PACKAGE -> {
                installStreamingZipUpdate(openInputStream(dsuInstallation.uri))
            }

            Type.URL -> {
                val url = URL(dsuInstallation.uri.toString())
                val connection = url.openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                installStreamingZipUpdate(connection.getInputStream())
            }

            else -> {}
        }
        if (!installationJob.isCancelled) {
            if (!finishInstallation()) {
                onInstallationError(InstallationStep.ERROR, "finishInstallation() returned false")
                return
            }
            AppLogger.i(tag, "Installation finished successfully")
            onInstallationSuccess()
        }
    }

    private fun installImages(images: List<ImagePartition>) {
        for (image in images) {
            if (isPartitionSupported(image.partitionName)) {
                installImage(image.partitionName, image.fileSize, image.uri)
            }
            if (installationJob.isCancelled) {
                remove()
            }
        }
    }

    private fun installImage(partitionName: String, uncompressedSize: Long, uri: Uri) {
        installImage(
            partitionName,
            uncompressedSize,
            openInputStream(uri),
        )
        if (installationJob.isCancelled) {
            remove()
        }
    }

    fun openInputStream(uri: Uri): InputStream {
        return requireNotNull(application.contentResolver.openInputStream(uri)) {
            "Unable to open input stream for uri: $uri"
        }
    }

    fun createNewPartition(partition: String, partitionSize: Long, readOnly: Boolean): Boolean {
        val result = createPartition(partition, partitionSize, readOnly)
        if (result != IGsiService.INSTALL_OK) {
            AppLogger.e(
                tag,
                "Failed to create partition",
                null,
                "partition" to partition,
                "partitionSize" to partitionSize,
                "readOnly" to readOnly,
                "errorCode" to result,
            )
            installationJob.cancel()
            onInstallationError(InstallationStep.ERROR_CREATE_PARTITION, partition)
            return false
        }
        return true
    }

    override fun invoke() {
        runCatching { startInstallation() }.onFailure {
            AppLogger.e(tag, "Unexpected installer failure", it)
            onInstallationError(
                InstallationStep.ERROR,
                it.message ?: "Unexpected installer failure",
            )
        }
    }
}
