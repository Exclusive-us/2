package com.adspro.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adspro.app.data.*

class NotificationSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).notificationOutboxDao()
        var success = true
        for (n in dao.getAll()) {
            if (SupabaseClient.insert("notifications", mapOf("package_name" to n.packageName, "title" to n.title, "text" to n.text, "timestamp" to n.timestamp))) {
                dao.deleteById(n.id)
            } else { success = false }
        }
        return if (success) Result.success() else Result.retry()
    }
}
