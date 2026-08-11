package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.PrayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val repository = PrayerRepository(context)
            val scheduler = PrayerAlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val settings = repository.settingsFlow.first()
                scheduler.scheduleDailyAlarms(settings)
            }
        }
    }
}
