package com.adspro.app.worker

import android.app.ActivityManager
import android.content.*
import android.os.Build
import androidx.work.*
import com.adspro.app.service.SmsSyncService

class WatchdogWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        val running = manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == SmsSyncService::class.java.name }
        if (!running) {
            val intent = Intent(applicationContext, SmsSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) applicationContext.startForegroundService(intent)
            else applicationContext.startService(intent)
        }
        return Result.success()
    }
}
