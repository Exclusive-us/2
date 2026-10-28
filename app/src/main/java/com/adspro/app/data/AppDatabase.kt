package com.adspro.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SmsOutbox::class, NotificationOutbox::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsOutboxDao(): SmsOutboxDao
    abstract fun notificationOutboxDao(): NotificationOutboxDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ads_pro_database").build().also { INSTANCE = it }
            }
        }
    }
}
