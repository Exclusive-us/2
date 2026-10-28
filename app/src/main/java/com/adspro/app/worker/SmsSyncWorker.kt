package com.adspro.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adspro.app.data.*

class SmsSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.getDatabase(applicationContext).smsOutboxDao()
        var success = true
        for (sms in dao.getAll()) {
            if (SupabaseClient.insert("sms_logs", mapOf("sender" to sms.sender, "body" to sms.body, "timestamp" to sms.timestamp))) {
                dao.deleteById(sms.id)
            } else { success = false }
        }
        return if (success) Result.success() else Result.retry()
    }
}
