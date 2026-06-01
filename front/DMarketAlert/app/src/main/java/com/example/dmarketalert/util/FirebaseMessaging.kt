package com.example.dmarketalert.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.dmarketalert.R
import androidx.core.app.NotificationCompat
import com.example.dmarketalert.model.local.NotificationEntity
import com.example.dmarketalert.repository.local.AppDatabase
import com.example.dmarketalert.view.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.remoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService: FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data.isNotEmpty()){
            val itemName = message.data["item_name"] ?: "Скін"
            val targetPrice = message.data["target_price"] ?: "0"
            val actualPrice = message.data["actual_price"] ?: "0"

            val title = "Target has been outbited!"
            val message = "$itemName has been outbited, price: $targetPrice. Your current price: $actualPrice"

            val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
            CoroutineScope(Dispatchers.IO).launch {
                dao.insertNotification(
                    NotificationEntity(
                        title = title,
                        message = message,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            sendSystemNotification(title, message)
        }
    }

    private fun sendSystemNotification(title: String, message: String){
        val channelId = "dmarket_alert_channel"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                channelId,
                "Dmarket Alert Notifications",
                NotificationManager.IMPORTANCE_HIGH //
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}