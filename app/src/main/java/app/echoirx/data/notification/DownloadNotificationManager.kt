package app.echoirx.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import app.echoirx.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "download_channel"
        const val GROUP_KEY = "download_group"
        const val SUMMARY_ID = 0
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private val activeDownloads = mutableSetOf<String>()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        title: String,
        contentText: String? = null,
        progress: Int? = null,
        indeterminate: Boolean = false,
        ongoing: Boolean = false,
        autoCancel: Boolean = false,
        category: String = NotificationCompat.CATEGORY_PROGRESS
    ) = NotificationCompat.Builder(context, CHANNEL_ID).apply {
        setContentTitle(title)
        setSmallIcon(R.drawable.ic_download)
        setGroup(GROUP_KEY)
        setCategory(category)
        setOngoing(ongoing)
        setAutoCancel(autoCancel)
        setOnlyAlertOnce(true)
        setSilent(true)
        contentText?.let { setContentText(it) }
        progress?.let { setProgress(100, it, indeterminate) }
    }.build()

    fun createDownloadNotification(
        downloadId: String,
        title: String,
        progress: Int,
        indeterminate: Boolean
    ): ForegroundInfo {
        activeDownloads.add(downloadId)

        val notification = buildNotification(
            title = title,
            progress = progress,
            indeterminate = indeterminate,
            ongoing = true
        )

        val notificationId = downloadId.hashCode()
        notificationManager.notify(notificationId, notification)
        updateSummaryNotification()

        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ForegroundInfo(notificationId, notification)
        } else {
            ForegroundInfo(notificationId, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }
    }

    fun updateDownloadProgress(
        downloadId: String,
        title: String,
        progress: Int,
        indeterminate: Boolean
    ) {
        val notification = buildNotification(
            title = title,
            progress = progress,
            indeterminate = indeterminate,
            ongoing = true
        )

        val notificationId = downloadId.hashCode()
        notificationManager.notify(notificationId, notification)
        updateSummaryNotification()
    }

    fun showCompletionNotification(downloadId: String, title: String) {
        activeDownloads.remove(downloadId)

        val notification = buildNotification(
            title = context.getString(R.string.notification_complete),
            contentText = title,
            autoCancel = true,
            category = NotificationCompat.CATEGORY_STATUS
        )

        notificationManager.notify(downloadId.hashCode(), notification)
        updateSummaryNotification()
    }

    fun showErrorNotification(downloadId: String, title: String) {
        activeDownloads.remove(downloadId)

        val notification = buildNotification(
            title = context.getString(R.string.notification_failed),
            contentText = title,
            autoCancel = true,
            category = NotificationCompat.CATEGORY_ERROR
        )

        notificationManager.notify(downloadId.hashCode(), notification)
        updateSummaryNotification()
    }

    fun cancelDownloadNotification(downloadId: String) {
        activeDownloads.remove(downloadId)
        notificationManager.cancel(downloadId.hashCode())
        updateSummaryNotification()
    }

    private fun updateSummaryNotification() {
        if (activeDownloads.isEmpty()) {
            notificationManager.cancel(SUMMARY_ID)
        } else {
            val summaryNotification = buildNotification(
                title = context.getString(R.string.notification_progress),
                contentText = context.getString(
                    R.string.notification_downloads_count,
                    activeDownloads.size
                ),
                ongoing = true
            ).apply {
                flags = flags or NotificationCompat.FLAG_GROUP_SUMMARY
            }

            notificationManager.notify(SUMMARY_ID, summaryNotification)
        }
    }
}