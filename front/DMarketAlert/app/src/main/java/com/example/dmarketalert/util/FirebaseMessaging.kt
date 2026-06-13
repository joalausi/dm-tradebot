package com.example.dmarketalert.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.dmarketalert.R
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dmarketalert.model.AppSettings
import com.example.dmarketalert.model.local.NotificationEntity
import com.example.dmarketalert.repository.local.AppDatabase
import com.example.dmarketalert.view.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.remoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (!SettingsManager.isNotificationsEnabled(applicationContext)) return

        if (remoteMessage.data.isNotEmpty()) {
            val itemName = remoteMessage.data["item_name"] ?: "Скін"
            val targetPrice = remoteMessage.data["target_price"] ?: "0"
            val actualPrice = remoteMessage.data["actual_price"] ?: "0"

            val title = "Target has been outbid!"
            val body = "$itemName has been outbid. Target price: $targetPrice. Your price: $actualPrice"

            val delayHours = SettingsManager.getNotificationDelay(applicationContext)

            if (delayHours > 0){
                scheduleDelayedNotification(title, body, delayHours)
            } else {
                processAndShowNotification(title, body)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun processAndShowNotification(title: String, message: String) {
        val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
        serviceScope.launch {
            dao.insertNotification(
                NotificationEntity(title = title, message = message, timestamp = System.currentTimeMillis())
            )
            val limit = SettingsManager.getNotificationLimit(applicationContext)
            if (limit > 0) dao.applyLimit(limit)
        }

        sendSystemNotification(title, message)
    }

    private fun scheduleDelayedNotification(title: String, message: String, delayHours: Int) {
        val inputData = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayHours.toLong(), TimeUnit.HOURS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    private fun sendSystemNotification(title: String, message: String) {
        val mode = SettingsManager.getNotificationMode(applicationContext)
        val channelId = when (mode) {
            AppSettings.NOTIFICATION_VIBRATION -> "dm_alert_vibration"
            AppSettings.NOTIFICATION_SILENT -> "dm_alert_silent"
            else -> "dm_alert_sound"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}