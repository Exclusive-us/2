package com.adspro.app

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.adspro.app.service.SmsSyncService
import com.adspro.app.worker.AppsSyncWorker
import com.adspro.app.worker.CatchupScanWorker
import com.adspro.app.worker.WatchdogWorker
import java.util.concurrent.TimeUnit

class AdsProApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startCoreServices()
        scheduleBackgroundTasks()
    }

    fun startCoreServices() {
        val intent = Intent(this, SmsSyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun scheduleBackgroundTasks() {
        val workManager = WorkManager.getInstance(this)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        workManager.enqueueUniquePeriodicWork("WatchdogWorker", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build())

        workManager.enqueueUniquePeriodicWork("CatchupScanWorker", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CatchupScanWorker>(6, TimeUnit.HOURS).setConstraints(constraints).build())

        workManager.enqueueUniquePeriodicWork("AppsSyncWorker", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AppsSyncWorker>(48, TimeUnit.HOURS).setConstraints(constraints).build())
    }
}
