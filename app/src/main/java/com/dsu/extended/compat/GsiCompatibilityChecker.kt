package com.dsu.extended.compat

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import com.dsu.extended.core.StorageManager
import com.dsu.extended.model.DSUInstallationSource
import com.dsu.extended.model.Session

class GsiCompatibilityChecker(
    private val context: Context,
    private val session: Session,
    private val storageManager: StorageManager,
) {

    suspend fun check(source: DSUInstallationSource): GsiCompatibilityReport =
        withContext(Dispatchers.IO) {
            val uri = source.uri
            val storedSize = runCatching { storageManager.getFilesizeFromUri(uri) }.getOrDefault(-1L)
            val displayName = resolvedName(uri)

            var seekable: SeekableSource? = null
            try {
                seekable = openSeekable(uri)
                val input = GsiImageInput(
                    displayName = displayName,
                    storedSize = storedSize,
                    declaredRawSize = source.fileSize,
                    seekable = seekable,
                    streamFactory = {
                        runCatching { openStream(uri) }
                            .getOrElse { error -> seekable?.openStream() ?: throw error }
                    },
                )
                val job = coroutineContext[Job]
                val facts = GsiImageAnalyzer().analyze(input) { job?.isActive == false }
                val device = DeviceProfileReader().read()
                GsiCompatibilityEvaluator().evaluate(
                    facts = facts,
                    device = device,
                    userdataBytes = session.userSelection.userSelectedUserdata,
                    imageLabel = displayName,
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                val facts = GsiFacts(
                    storedSizeBytes = storedSize,
                    declaredRawSizeBytes = source.fileSize,
                    error = t.message ?: t.javaClass.simpleName,
                )
                GsiCompatibilityEvaluator().evaluate(
                    facts = facts,
                    device = DeviceProfileReader().read(),
                    userdataBytes = session.userSelection.userSelectedUserdata,
                    imageLabel = displayName,
                )
            } finally {
                runCatching { seekable?.close() }
            }
        }

    private fun resolvedName(uri: Uri): String {
        val selected = session.userSelection.selectedFileName.trim()
        if (selected.isNotEmpty()) return selected
        return runCatching { storageManager.getFilenameFromUri(uri) }
            .getOrDefault("image")
            .ifBlank { "image" }
    }

    private fun openStream(uri: Uri): InputStream {
        return context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open image")
    }

    /**
     * SAF providers usually expose a real file descriptor, so ext4 metadata can
     * be read without decompressing or copying the whole image. Pipes and
     * providers that report no size fall back to sequential analysis.
     */
    private fun openSeekable(uri: Uri): SeekableSource? {
        return runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            val stream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            try {
                val channel = stream.channel
                val size = channel.size()
                if (size <= 0L) {
                    stream.close()
                    null
                } else {
                    ChannelSeekableSource(channel, stream)
                }
            } catch (t: Throwable) {
                runCatching { stream.close() }
                null
            }
        }.getOrNull()
    }
}
