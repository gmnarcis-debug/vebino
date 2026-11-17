package com.example.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.example.MainActivity
import com.example.domain.model.BuildStatus
import red.line.vebino.R

/**
 * Progress + result notifications for background builds.
 * Text is passed in already localized by the caller (see [BuildNotifier.Texts]).
 */
class BuildNotifier(private val context: Context) {

    data class Texts(
        val channelName: String,
        val channelDesc: String,
        val running: String,
        val queued: String,
        val succeeded: String,
        val failed: String,
        val cancelled: String,
        val cancelAction: String,
        val tapToOpen: String,
    )

    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannel(texts: Texts) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sys = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val progress = NotificationChannel(CHANNEL_PROGRESS, texts.channelName, NotificationManager.IMPORTANCE_LOW)
        progress.description = texts.channelDesc
        progress.setShowBadge(false)
        sys.createNotificationChannel(progress)
        val result = NotificationChannel(CHANNEL_RESULT, texts.channelName, NotificationManager.IMPORTANCE_DEFAULT)
        result.description = texts.channelDesc
        sys.createNotificationChannel(result)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun progressNotification(
        taskId: Long,
        appName: String,
        stepLabel: String,
        percent: Int,
        queued: Boolean,
        texts: Texts,
        cancelIntent: PendingIntent?,
    ) = NotificationCompat.Builder(context, CHANNEL_PROGRESS)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("${if (queued) texts.queued else texts.running} • $appName")
        .setContentText(stepLabel)
        .setProgress(100, percent.coerceIn(0, 100), queued)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setContentIntent(openAppIntent())
        .apply { if (cancelIntent != null) addAction(0, texts.cancelAction, cancelIntent) }
        .build()

    fun foregroundInfo(taskId: Long, notification: android.app.Notification): ForegroundInfo {
        val id = NOTIFICATION_BASE + (taskId % 100000).toInt()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    fun showResult(taskId: Long, appName: String, status: BuildStatus, detail: String?, texts: Texts) {
        if (!canPost()) return
        val title = when (status) {
            BuildStatus.SUCCEEDED -> texts.succeeded
            BuildStatus.CANCELLED -> texts.cancelled
            else -> texts.failed
        }
        val n = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$title • $appName")
            .setContentText(detail?.takeIf { it.isNotBlank() } ?: texts.tapToOpen)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail?.takeIf { it.isNotBlank() } ?: texts.tapToOpen))
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        runCatching { manager.notify(NOTIFICATION_BASE + 500000 + (taskId % 100000).toInt(), n) }
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_PROGRESS = "webino_build_progress"
        const val CHANNEL_RESULT = "webino_build_result"
        private const val NOTIFICATION_BASE = 4200
    }
}
