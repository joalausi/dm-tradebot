package com.example.dmarketalert.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import com.example.dmarketalert.R
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dmarketalert.model.local.NotificationEntity
import com.example.dmarketalert.repository.local.AppDatabase
import com.example.dmarketalert.view.MainActivity

class NotificationWorker(appContext: Context, workParams: WorkerParameters) : CoroutineWorker(appContext, workParams) {

    override suspend fun doWork() : Result {
        val title = inputData.getString("title") ?: return Result.failure()
        val message = inputData.getString("message") ?: return Result.failure()

        val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
        dao.insertNotification(
            NotificationEntity(title = title, message = message, timestamp = System.currentTimeMillis())
        )

        val mode = SettingsManager.getNotificationMode(applicationContext)

        val channelId = when (mode) {
            "vibration" -> "dm_alert_vibration"
            "silent" -> "dm_alert_silent"
            else -> "dm_alert_sound"
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())

        return Result.success()
    }
}