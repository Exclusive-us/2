package com.adspro.app.worker

import android.content.Context
import android.content.pm.*
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adspro.app.data.SupabaseClient

class AppsSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val pm = applicationContext.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        var count = 0
        for (app in apps) {
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                if (SupabaseClient.insert("installed_apps", mapOf("app_name" to pm.getApplicationLabel(app).toString(), "package_name" to app.packageName, "updated_at" to System.currentTimeMillis()))) {
                    count++
                }
            }
        }
        return if (count > 0) Result.success() else Result.retry()
    }
}
