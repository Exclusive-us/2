package com.adspro.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_outbox")
data class SmsOutbox(@PrimaryKey(autoGenerate = true) val id: Long = 0, val sender: String, val body: String, val timestamp: Long)

@Entity(tableName = "notification_outbox")
data class NotificationOutbox(@PrimaryKey(autoGenerate = true) val id: Long = 0, val packageName: String, val title: String, val text: String, val timestamp: Long)
