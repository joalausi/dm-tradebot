package com.example.dmarketalert.repository.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dmarketalert.model.local.NotificationEntity
import android.content.Context

@Database(entities = [NotificationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase{
            return INSTANCE?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dmarket_alert_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}