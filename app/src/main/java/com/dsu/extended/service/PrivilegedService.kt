package com.dsu.extended.service

import android.app.IActivityManager
import android.content.Intent
import android.content.pm.IPackageManager
import android.gsi.GsiProgress
import android.gsi.IGsiService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemProperties
import android.os.image.IDynamicSystemService
import android.os.storage.IStorageManager
import android.gsi.IImageService
import android.gsi.MappedImage
import android.os.StatFs
import android.os.storage.VolumeInfo
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import org.lsposed.hiddenapibypass.HiddenApiBypass
import com.dsu.extended.BuildConfig
import com.dsu.extended.IPartitionTransferListener
import com.dsu.extended.IPrivilegedService
import com.dsu.extended.util.PartitionResult

class PrivilegedService : IPrivilegedService.Stub() {

    override fun exit() {
        destroy()
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun getBinder(service: String): IBinder {
        val serviceManager = Class.forName("android.os.ServiceManager")
        val binder = HiddenApiBypass.invoke(serviceManager, null, "getService", service)
        return binder as IBinder
    }

    fun setProp(key: String, value: String) {
        try {
            SystemProperties.set(key, value)
        } catch (e: Exception) {
            Log.w(BuildConfig.APPLICATION_ID, e.stackTraceToString())
        }
    }

    override fun setDynProp() {
        setProp("persist.sys.fflag.override.settings_dynamic_system", "true")
    }

    override fun getUid(): Int {
        return Process.myUid()
    }

    private var ACTIVITY_MANAGER: IActivityManager? = null

    private fun requiresActivityManager() {
        if (ACTIVITY_MANAGER == null) {
            ACTIVITY_MANAGER = IActivityManager.Stub.asInterface(getBinder("activity"))
        }
    }

    override fun startActivity(intent: Intent?) {
        requiresActivityManager()
        val callerPackage =
            if (uid == 2000 || uid == 0) "com.android.shell" else BuildConfig.APPLICATION_ID

        if (Build.VERSION.SDK_INT > 29) {
            ACTIVITY_MANAGER!!.startActivityAsUserWithFeature(
                null,
                callerPackage,
                null,
                intent,
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                0,
            )
        } else {
            ACTIVITY_MANAGER!!.startActivityAsUser(
                null,
                callerPackage,
                intent,
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                0,
            )
        }
    }

    override fun forceStopPackage(packageName: String?) {
        requiresActivityManager()
        ACTIVITY_MANAGER!!.forceStopPackage(packageName, 0)
    }

    private var PACKAGE_MANAGER: IPackageManager? = null

    private fun requiresPackageManager() {
        if (PACKAGE_MANAGER == null) {
            PACKAGE_MANAGER = IPackageManager.Stub.asInterface(getBinder("package"))
        }
    }

    override fun grantPermission(permissionName: String?) {
        requiresPackageManager()
        PACKAGE_MANAGER!!.grantRuntimePermission(BuildConfig.APPLICATION_ID, permissionName, 0)
    }

    private var STORAGE_MANAGER: IStorageManager? = null

    private fun requiresStorageManager() {
        if (STORAGE_MANAGER == null) {
            STORAGE_MANAGER = IStorageManager.Stub.asInterface(getBinder("mount"))
        }
    }

    override fun getVolumes(): List<VolumeInfo> {
        requiresStorageManager()
        val vols = ArrayList<VolumeInfo>()
        vols.addAll(STORAGE_MANAGER!!.getVolumes(0))
        return vols
    }

    override fun unmount(volId: String?) {
        requiresStorageManager()
        STORAGE_MANAGER!!.unmount(volId)
    }

    override fun mount(volId: String?) {
        requiresStorageManager()
        STORAGE_MANAGER!!.mount(volId)
    }

    /**
     * Dynamic System Service
     *
     * Most methods are using @EnforcePermission("MANAGE_DYNAMIC_SYSTEM")
     * they are only accessible via root or as system app (proper installed)
     * Shizuku is able to call those methods, but they won't work as shell (2000)
     * since MANAGE_DYNAMIC_SYSTEM is required, and shell does not have it
     *
     * On stock Android, shell is able to install GSIs via DSU over Dynamic System Updates app
     * that has MANAGE_DYNAMIC_SYSTEM permission, shell has only INSTALL_DYNAMIC_SYSTEM
     */

    private var DYNAMIC_SYSTEM: IDynamicSystemService? = null

    private fun requiresDynamicSystem() {
        if (DYNAMIC_SYSTEM == null) {
            DYNAMIC_SYSTEM = IDynamicSystemService.Stub.asInterface(getBinder("dynamic_system"))
        }
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun closePartition(): Boolean {
        if (Build.VERSION.SDK_INT <= 30) {
            // Android R does not seem to close partition?
            // closePartition() was implemented on S
            return true
        }
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.closePartition()
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun finishInstallation(): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.finishInstallation()
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun getInstallationProgress(): GsiProgress? {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.installationProgress
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun abort(): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.abort()
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun isEnabled(): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.isEnabled
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun remove(): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.remove()
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun setEnable(enable: Boolean, oneShot: Boolean): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.setEnable(enable, oneShot)
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun startInstallation(dsuSlot: String?): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.startInstallation(dsuSlot)
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun createPartition(name: String?, size: Long, readOnly: Boolean): Int {
        requiresDynamicSystem()
        // Below T, createPartition returns boolean
        if (Build.VERSION.SDK_INT < 33) {
            val result = HiddenApiBypass.invoke(
                DYNAMIC_SYSTEM!!.javaClass,
                DYNAMIC_SYSTEM!!,
                "createPartition",
                name,
                size,
                readOnly,
            )
            return if (result as Boolean) IGsiService.INSTALL_OK else IGsiService.INSTALL_ERROR_GENERIC
        }
        return DYNAMIC_SYSTEM!!.createPartition(name, size, readOnly)
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun setAshmem(fd: ParcelFileDescriptor?, size: Long): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.setAshmem(fd, size)
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun submitFromAshmem(bytes: Long): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.submitFromAshmem(bytes)
    }

    // REQUIRES MANAGE_DYNAMIC_SYSTEM
    override fun suggestScratchSize(): Long {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.suggestScratchSize()
    }

    override fun isInUse(): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.isInUse
    }

    override fun isInstalled(): Boolean {
        requiresDynamicSystem()
        return DYNAMIC_SYSTEM!!.isInstalled
    }

    private var GSI_SERVICE: IGsiService? = null

    private fun getBinderOrNull(service: String): IBinder? {
        return runCatching { getBinder(service) }.getOrNull()
    }

    private fun requiresGsiService(): IGsiService {
        GSI_SERVICE?.let { return it }
        val binder = getBinderOrNull("gsiservice")
            ?: run {
                setProp("ctl.start", "gsid")
                val deadline = System.currentTimeMillis() + 10_000L
                var found: IBinder? = null
                while (System.currentTimeMillis() < deadline && found == null) {
                    Thread.sleep(100L)
                    found = getBinderOrNull("gsiservice")
                }
                found ?: throw IllegalStateException("gsiservice is unavailable")
            }
        return IGsiService.Stub.asInterface(binder).also { GSI_SERVICE = it }
    }

    override fun getImagePrefixes(): List<String> {
        val gsi = runCatching { requiresGsiService() }.getOrNull()
            ?: return emptyList()
        val prefixes = buildList {
            runCatching { gsi.installedDsuSlots }.getOrDefault(emptyList()).forEach { slot ->
                add("$slot/$slot/")
            }
            runCatching { gsi.activeDsuSlot?.takeIf { it.isNotEmpty() } }.getOrNull()?.let { slot ->
                add("$slot/$slot/")
            }
            runCatching { gsi.installedGsiImageDir.orEmpty() }.getOrNull()
                ?.removePrefix("/data/gsi/")
                ?.trim('/')
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("$it/") }
            add("dsu/dsu/")
        }
        return prefixes.distinct()
    }

    override fun getDsuImages(prefix: String?): List<String> {
        if (prefix.isNullOrEmpty()) return emptyList()
        val gsi = runCatching { requiresGsiService() }.getOrNull() ?: return emptyList()
        return runCatching { gsi.openImageService(prefix).allBackingImages }.getOrDefault(emptyList())
    }

    override fun deleteDsuImage(prefix: String?, imageName: String?): Int {
        val name = imageName ?: return PartitionResult.INVALID_NAME
        if (!isValidImageName(name) || prefix.isNullOrEmpty()) return PartitionResult.INVALID_NAME
        return runGsiOperation {
            val imageService = requiresGsiService().openImageService(prefix)
            if (!imageService.backingImageExists(name)) {
                return@runGsiOperation PartitionResult.NOT_FOUND
            }
            if (imageService.isImageMapped(name)) {
                imageService.unmapImageDevice(name)
            }
            imageService.deleteBackingImage(name)
            PartitionResult.OK
        }
    }

    private var transferThread: ExecutorService? = null
    private var transferCancelled = AtomicBoolean(false)
    private var transferListener: IPartitionTransferListener? = null

    private fun transferExecutor(): ExecutorService {
        if (transferThread == null) {
            synchronized(this) {
                if (transferThread == null) {
                    transferThread = Executors.newSingleThreadExecutor { runnable ->
                        Thread(runnable, "dsu-image-transfer").apply { isDaemon = true }
                    }
                }
            }
        }
        return transferThread!!
    }

    override fun startAddDsuImage(
        prefix: String?,
        imageName: String?,
        imageFd: ParcelFileDescriptor?,
        imageSize: Long,
        readOnly: Boolean,
        listener: IPartitionTransferListener?,
    ): Boolean = startWriteTransfer(prefix, imageName, imageFd, imageSize, readOnly, listener, replaceExisting = false)

    override fun startReplaceDsuImage(
        prefix: String?,
        imageName: String?,
        imageFd: ParcelFileDescriptor?,
        imageSize: Long,
        readOnly: Boolean,
        listener: IPartitionTransferListener?,
    ): Boolean = startWriteTransfer(prefix, imageName, imageFd, imageSize, readOnly, listener, replaceExisting = true)

    override fun startExportDsuImage(
        prefix: String?,
        imageName: String?,
        imageFd: ParcelFileDescriptor?,
        listener: IPartitionTransferListener?,
    ): Boolean {
        val name = imageName ?: run {
            imageFd?.close()
            return false
        }
        if (!isValidImageName(name) || prefix.isNullOrEmpty() || imageFd == null) {
            imageFd?.close()
            return false
        }
        synchronized(this) {
            if (isTransferRunning()) {
                imageFd.close()
                return false
            }
            beginTransfer(listener)
        }
        transferExecutor().execute {
            val result = runGsiOperation {
                val gsi = requiresGsiService()
                val imageService = gsi.openImageService(prefix)
                if (!imageService.backingImageExists(imageName)) {
                    return@runGsiOperation PartitionResult.NOT_FOUND
                }
                if (imageService.isImageMapped(name)) {
                    imageService.unmapImageDevice(name)
                }
                val mappedImage = MappedImage()
                imageService.mapImageDevice(name, PartitionResult.MAP_TIMEOUT_MS, mappedImage)
                try {
                    exportToStream(imageService, name, mappedImage.path, imageFd)
                } finally {
                    imageService.unmapImageDevice(imageName)
                }
                PartitionResult.OK
            }
            finishTransfer(result)
        }
        return true
    }

    override fun cancelDsuImageTransfer() {
        transferCancelled.set(true)
    }

    private fun isTransferRunning(): Boolean {
        val executor = transferThread ?: return false
        return !executor.isShutdown && !transferDone.get()
    }

    private var transferDone = AtomicBoolean(true)

    private fun beginTransfer(listener: IPartitionTransferListener?) {
        transferCancelled.set(false)
        transferDone.set(false)
        transferListener = listener
    }

    private fun finishTransfer(resultCode: Int) {
        val listener = transferListener
        transferListener = null
        transferDone.set(true)
        runCatching { listener?.onCompleted(resultCode) }
    }

    private fun startWriteTransfer(
        prefix: String?,
        imageName: String?,
        imageFd: ParcelFileDescriptor?,
        imageSize: Long,
        readOnly: Boolean,
        listener: IPartitionTransferListener?,
        replaceExisting: Boolean,
    ): Boolean {
        val fd: ParcelFileDescriptor = imageFd ?: return false
        val name = imageName ?: run {
            fd.close()
            return false
        }
        if (!isValidImageName(name) || prefix.isNullOrEmpty()) {
            fd.close()
            return false
        }
        if (imageSize <= 0 || imageSize % 512L != 0L) {
            fd.close()
            return false
        }
        synchronized(this) {
            if (isTransferRunning()) {
                fd.close()
                return false
            }
            beginTransfer(listener)
        }
        transferExecutor().execute {
            val result = runGsiOperation {
                val gsi = requiresGsiService()
                val dataDir = File("/data/gsi", prefix)
                val freeBytes = runCatching { StatFs(dataDir.absolutePath).availableBytes }.getOrDefault(-1L)
                if (freeBytes >= 0 && imageSize > freeBytes - 512L * 1024 * 1024) {
                    return@runGsiOperation PartitionResult.NO_SPACE
                }
                val flags =
                    if (readOnly) IImageService.CREATE_IMAGE_READONLY else IImageService.CREATE_IMAGE_DEFAULT
                writeBackingImage(gsi, prefix, name, fd, imageSize, flags, replaceExisting)
                PartitionResult.OK
            }
            finishTransfer(result)
        }
        return true
    }

    private fun writeBackingImage(
        gsi: IGsiService,
        prefix: String,
        imageName: String,
        imageFd: ParcelFileDescriptor,
        imageSize: Long,
        flags: Int,
        replaceExisting: Boolean,
    ) {
        val imageService = gsi.openImageService(prefix)
        val exists = imageService.backingImageExists(imageName)
        if (exists && !replaceExisting) {
            throw IllegalStateException("duplicate")
        }
        if (imageService.isImageMapped(imageName)) {
            imageService.unmapImageDevice(imageName)
        }
        if (exists) {
            imageService.deleteBackingImage(imageName)
        }

        var created = false
        var mapped = false
        val mappedImage = MappedImage()
        try {
            imageService.createBackingImage(imageName, imageSize, flags, null)
            created = true
            imageService.mapImageDevice(imageName, PartitionResult.MAP_TIMEOUT_MS, mappedImage)
            mapped = true
            copyStreamToDevice(imageFd, mappedImage.path, imageSize)
        } catch (e: Exception) {
            runCatching { if (mapped) imageService.unmapImageDevice(imageName) }
            runCatching { if (created) imageService.deleteBackingImage(imageName) }
            throw e
        } finally {
            runCatching { if (mapped) imageService.unmapImageDevice(imageName) }
            imageFd.close()
        }
    }

    private fun exportToStream(
        imageService: IImageService,
        imageName: String,
        mappedPath: String,
        imageFd: ParcelFileDescriptor,
    ) {
        val buffer = ByteArray(PartitionResult.COPY_BUFFER_BYTES)
        FileInputStream(mappedPath).use { input ->
            FileOutputStream(imageFd.fileDescriptor).use { output ->
                var copied = 0L
                var sinceProgress = 0L
                while (true) {
                    if (transferCancelled.get()) {
                        throw IllegalStateException("cancelled")
                    }
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read.toLong()
                    sinceProgress += read.toLong()
                    if (sinceProgress >= PartitionResult.BYTES_PER_PROGRESS_STEP) {
                        sinceProgress = 0L
                        transferListener?.onProgress(copied, -1L)
                    }
                }
                output.fd.sync()
                transferListener?.onProgress(copied, copied)
            }
        }
    }

    private fun copyStreamToDevice(
        imageFd: ParcelFileDescriptor,
        mappedPath: String,
        totalBytes: Long,
    ) {
        val buffer = ByteArray(PartitionResult.COPY_BUFFER_BYTES)
        FileInputStream(imageFd.fileDescriptor).use { input ->
            FileOutputStream(mappedPath).use { output ->
                var copied = 0L
                var sinceProgress = 0L
                while (copied < totalBytes) {
                    if (transferCancelled.get()) {
                        throw IllegalStateException("cancelled")
                    }
                    val remaining = totalBytes - copied
                    val request = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = input.read(buffer, 0, request)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read.toLong()
                    sinceProgress += read.toLong()
                    if (sinceProgress >= PartitionResult.BYTES_PER_PROGRESS_STEP) {
                        sinceProgress = 0L
                        transferListener?.onProgress(copied, totalBytes)
                    }
                }
                output.fd.sync()
                if (copied != totalBytes) {
                    throw IllegalStateException("short copy: $copied of $totalBytes")
                }
                transferListener?.onProgress(copied, totalBytes)
            }
        }
    }

    private inline fun runGsiOperation(block: () -> Int): Int {
        return try {
            block()
        } catch (e: IllegalStateException) {
            when (e.message) {
                "duplicate" -> PartitionResult.DUPLICATE
                "cancelled" -> PartitionResult.CANCELLED
                else -> {
                    Log.e(BuildConfig.APPLICATION_ID, "partition operation failed", e)
                    PartitionResult.IO_ERROR
                }
            }
        } catch (e: Exception) {
            Log.e(BuildConfig.APPLICATION_ID, "partition operation failed", e)
            PartitionResult.SERVICE_UNAVAILABLE
        }
    }

    private fun isValidImageName(imageName: String?): Boolean {
        return !imageName.isNullOrBlank() && IMAGE_NAME_REGEX.matches(imageName)
    }

    private companion object {
        val IMAGE_NAME_REGEX = Regex("[A-Za-z0-9_.-]+")
    }
}