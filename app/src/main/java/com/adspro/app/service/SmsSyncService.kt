package com.adspro.app.service

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat

class SmsSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("ads_pro_silent_channel", "Ads Pro Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "ads_pro_silent_channel")
            .setContentTitle("Ads Pro Service").setContentText("Background sync active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).build()
        startForeground(888, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onTaskRemoved(rootIntent: Intent?) { scheduleRestart(); super.onTaskRemoved(rootIntent) }
    override fun onDestroy() { scheduleRestart(); super.onDestroy() }

    private fun scheduleRestart() {
        val intent = Intent(applicationContext, com.adspro.app.receiver.BootReceiver::class.java).apply { action = "com.adspro.app.ACTION_RESTART_SERVICE" }
        val pi = PendingIntent.getBroadcast(applicationContext, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000, pi)
    }
}
