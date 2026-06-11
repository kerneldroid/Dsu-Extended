package com.dsu.extended.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dsu.extended.MainActivity
import com.dsu.extended.R
import com.dsu.extended.preparation.InstallationStep

class InstallationLiveUpdateNotifier(
    private val context: Context,
) {

    companion object {
        private const val CHANNEL_ID = "dsu_installation_live_updates"
        private const val NOTIFICATION_ID = 0x4445
    }

    private val tag = this.javaClass.simpleName
    private val manager = NotificationManagerCompat.from(context)

    fun showProgress(
        step: InstallationStep,
        progress: Float,
        partition: String,
    ) {
        if (!canPostNotifications()) {
            return
        }
        ensureChannel()

        val safeProgress = progress.coerceIn(0f, 1f)
        val progressPercent = (safeProgress * 100).toInt().coerceIn(0, 100)
        val isIndeterminate = progressPercent == 0

        val builder =
            baseBuilder(
                title = context.getString(R.string.live_update_notification_title),
                text = stepToNotificationText(step, partition),
                ongoing = true,
            ).setProgress(100, progressPercent, isIndeterminate)

        if (Build.VERSION.SDK_INT >= 36) {
            builder
                .setStyle(NotificationCompat.ProgressStyle().setProgress(progressPercent))
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(context.getString(R.string.live_update_notification_short_text, progressPercent))
        }

        notify(builder)
    }

    fun showSuccess(canRebootToDsu: Boolean) {
        if (!canPostNotifications()) {
            return
        }
        ensureChannel()

        val text =
            if (canRebootToDsu) {
                context.getString(R.string.live_update_notification_success_reboot)
            } else {
                context.getString(R.string.live_update_notification_success)
            }

        val builder =
            baseBuilder(
                title = context.getString(R.string.installation),
                text = text,
                ongoing = false,
            )
                .setOnlyAlertOnce(false)
                .setProgress(0, 0, false)
                .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= 36) {
            builder.setRequestPromotedOngoing(false)
        }

        notify(builder)
    }

    fun showError(errorText: String) {
        if (!canPostNotifications()) {
            return
        }
        ensureChannel()

        val builder =
            baseBuilder(
                title = context.getString(R.string.error),
                text = errorText.ifBlank { context.getString(R.string.live_update_notification_error) },
                ongoing = false,
            )
                .setOnlyAlertOnce(false)
                .setProgress(0, 0, false)
                .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= 36) {
            builder.setRequestPromotedOngoing(false)
        }

        notify(builder)
    }

    fun cancel() {
        manager.cancel(NOTIFICATION_ID)
    }

    private fun notify(builder: NotificationCompat.Builder) {
        runCatching {
            manager.notify(NOTIFICATION_ID, builder.build())
        }.onFailure {
            AppLogger.w(tag, "Failed to post installation live update notification", "error" to (it.message ?: "unknown"))
        }
    }

    private fun baseBuilder(
        title: String,
        text: String,
        ongoing: Boolean,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon_mono)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(buildLaunchIntent())
    }

    private fun buildLaunchIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.live_update_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.live_update_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission =
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                return false
            }
        }
        return manager.areNotificationsEnabled()
    }

    private fun stepToNotificationText(
        step: InstallationStep,
        partition: String,
    ): String {
        return when (step) {
            InstallationStep.COPYING_FILE -> context.getString(R.string.copying_file)
            InstallationStep.DECOMPRESSING_XZ -> context.getString(R.string.decompressing_xz)
            InstallationStep.COMPRESSING_TO_GZ -> context.getString(R.string.compressing_to_gz)
            InstallationStep.DECOMPRESSING_GZIP -> context.getString(R.string.decompressing_gz)
            InstallationStep.EXTRACTING_FILE -> context.getString(R.string.extracting_file)
            InstallationStep.VALIDATING_IMAGE -> context.getString(R.string.validating_image)
            InstallationStep.CREATING_PARTITION ->
                if (partition.isNotBlank()) {
                    context.getString(R.string.creating_partition, partition)
                } else {
                    context.getString(R.string.processing)
                }

            InstallationStep.INSTALLING,
            InstallationStep.INSTALLING_ROOTED,
            InstallationStep.PROCESSING_LOG_READABLE,
            -> if (partition.isNotBlank()) {
                context.getString(R.string.installing_partition, partition)
            } else {
                context.getString(R.string.installing)
            }

            InstallationStep.WAITING_USER_CONFIRMATION -> context.getString(R.string.installation_prompt)
            InstallationStep.PROCESSING -> context.getString(R.string.processing)
            else -> context.getString(R.string.processing)
        }
    }
}
