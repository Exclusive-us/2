package com.adspro.app.receiver

import android.content.*
import com.adspro.app.AdsProApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent.action == "com.adspro.app.ACTION_RESTART_SERVICE") {
            (context.applicationContext as? AdsProApplication)?.startCoreServices()
        }
    }
}
