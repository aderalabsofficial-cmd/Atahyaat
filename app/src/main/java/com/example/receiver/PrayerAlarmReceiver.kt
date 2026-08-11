package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarm.PrayerNotificationHelper
import com.example.data.AppDatabase
import com.example.data.PrayerLogEntity
import com.example.data.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.ACTION_PRAYER_ALARM"
        const val ACTION_MARK_COMPLETED = "com.example.ACTION_MARK_COMPLETED"
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_FORMATTED_TIME = "extra_formatted_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: return
        val prayerType = PrayerType.fromKey(prayerKey)

        when (intent.action) {
            ACTION_PRAYER_ALARM -> {
                val formattedTime = intent.getStringExtra(EXTRA_FORMATTED_TIME) ?: ""
                val notificationHelper = PrayerNotificationHelper(context)
                notificationHelper.showPrayerNotification(prayerType, formattedTime)
            }
            ACTION_MARK_COMPLETED -> {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val db = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val existing = db.prayerDao().getLog(todayStr, prayerKey)
                    if (existing == null || !existing.isCompleted) {
                        db.prayerDao().insertLog(
                            PrayerLogEntity(
                                dateString = todayStr,
                                prayerKey = prayerKey,
                                isCompleted = true,
                                completedAtMillis = System.currentTimeMillis(),
                                isOnTime = true
                            )
                        )
                    }
                }
            }
        }
    }
}
