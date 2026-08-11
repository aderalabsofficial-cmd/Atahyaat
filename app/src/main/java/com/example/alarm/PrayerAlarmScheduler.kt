package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AlarmNotificationType
import com.example.data.PrayerSettings
import com.example.data.PrayerTimeCalculator
import com.example.receiver.PrayerAlarmReceiver
import java.util.Calendar

class PrayerAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val calculator = PrayerTimeCalculator()

    fun scheduleDailyAlarms(settings: PrayerSettings) {
        val calendar = Calendar.getInstance()
        val prayerTimes = calculator.calculateTimes(
            calendar = calendar,
            latitude = settings.latitude,
            longitude = settings.longitude,
            method = settings.method,
            customOffsets = settings.offsets
        )

        val nowMillis = System.currentTimeMillis()

        for ((type, calcTime) in prayerTimes) {
            val notifyType = settings.notifications[type.key] ?: AlarmNotificationType.SOUND_AND_NOTIFICATION
            if (notifyType == AlarmNotificationType.OFF) {
                cancelAlarm(type.key, type.ordinal)
                continue
            }

            var triggerTime = calcTime.timestampMillis
            if (triggerTime < nowMillis) {
                // If time already passed today, schedule for tomorrow
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val tomorrowTimes = calculator.calculateTimes(
                    calendar = tomorrow,
                    latitude = settings.latitude,
                    longitude = settings.longitude,
                    method = settings.method,
                    customOffsets = settings.offsets
                )
                triggerTime = tomorrowTimes[type]?.timestampMillis ?: (triggerTime + 86400000L)
            }

            scheduleExactAlarm(
                prayerKey = type.key,
                requestCode = type.ordinal,
                formattedTime = calcTime.formattedTime,
                triggerTimeMillis = triggerTime
            )
        }
    }

    private fun scheduleExactAlarm(
        prayerKey: String,
        requestCode: Int,
        formattedTime: String,
        triggerTimeMillis: Long
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, prayerKey)
            putExtra(PrayerAlarmReceiver.EXTRA_FORMATTED_TIME, formattedTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if schedule exact alarm permission not granted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(prayerKey: String, requestCode: Int) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, prayerKey)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
