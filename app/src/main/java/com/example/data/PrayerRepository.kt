package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.alarm.PrayerAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StreakMetrics(
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val totalPrayersCompleted: Int = 0,
    val todayCompletedCount: Int = 0,
    val totalObligatoryToday: Int = 5,
    val isTodayFullyCompleted: Boolean = false,
    val last30DaysStatus: List<Pair<String, Boolean>> = emptyList() // Date string to fully completed boolean
)

class PrayerRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.prayerDao()
    private val alarmScheduler = PrayerAlarmScheduler(context)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("atahyaat_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<PrayerSettings> = _settingsFlow.asStateFlow()

    val allLogsFlow: Flow<List<PrayerLogEntity>> = dao.getAllLogsFlow()
    val totalCompletedCountFlow: Flow<Int> = dao.getTotalCompletedCountFlow()
    val tasbihRecordsFlow: Flow<List<TasbihRecordEntity>> = dao.getTasbihRecordsFlow()

    fun getLogsForDateFlow(dateString: String): Flow<List<PrayerLogEntity>> {
        return dao.getLogsForDate(dateString)
    }

    val streakMetricsFlow: Flow<StreakMetrics> = dao.getAllLogsFlow().map { logs ->
        calculateStreakMetrics(logs)
    }

    private fun loadSettings(): PrayerSettings {
        val locationName = prefs.getString("location_name", "Makkah, Saudi Arabia") ?: "Makkah, Saudi Arabia"
        val latitude = prefs.getFloat("latitude", 21.4225f).toDouble()
        val longitude = prefs.getFloat("longitude", 39.8262f).toDouble()
        val timeZoneId = prefs.getString("timezone_id", "Asia/Riyadh") ?: "Asia/Riyadh"
        val methodId = prefs.getString("method_id", PrayerScheduleMethod.UMM_AL_QURA.id) ?: PrayerScheduleMethod.UMM_AL_QURA.id
        val themeModeName = prefs.getString("theme_mode", DarkThemeMode.SYSTEM.name) ?: DarkThemeMode.SYSTEM.name

        val offsets = mapOf(
            "fajr" to prefs.getInt("offset_fajr", 0),
            "dhuhr" to prefs.getInt("offset_dhuhr", 0),
            "asr" to prefs.getInt("offset_asr", 0),
            "maghrib" to prefs.getInt("offset_maghrib", 0),
            "isha" to prefs.getInt("offset_isha", 0)
        )

        val notifications = mapOf(
            "fajr" to AlarmNotificationType.fromName(prefs.getString("notif_fajr", AlarmNotificationType.SOUND_AND_NOTIFICATION.name)!!),
            "dhuhr" to AlarmNotificationType.fromName(prefs.getString("notif_dhuhr", AlarmNotificationType.SOUND_AND_NOTIFICATION.name)!!),
            "asr" to AlarmNotificationType.fromName(prefs.getString("notif_asr", AlarmNotificationType.SOUND_AND_NOTIFICATION.name)!!),
            "maghrib" to AlarmNotificationType.fromName(prefs.getString("notif_maghrib", AlarmNotificationType.SOUND_AND_NOTIFICATION.name)!!),
            "isha" to AlarmNotificationType.fromName(prefs.getString("notif_isha", AlarmNotificationType.SOUND_AND_NOTIFICATION.name)!!),
            "tahajjud" to AlarmNotificationType.fromName(prefs.getString("notif_tahajjud", AlarmNotificationType.NOTIFICATION_ONLY.name)!!),
            "duha" to AlarmNotificationType.fromName(prefs.getString("notif_duha", AlarmNotificationType.NOTIFICATION_ONLY.name)!!)
        )

        return PrayerSettings(
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            timeZoneId = timeZoneId,
            method = PrayerScheduleMethod.fromId(methodId),
            themeMode = DarkThemeMode.fromName(themeModeName),
            offsets = offsets,
            notifications = notifications
        )
    }

    suspend fun updateSettings(newSettings: PrayerSettings) {
        prefs.edit().apply {
            putString("location_name", newSettings.locationName)
            putFloat("latitude", newSettings.latitude.toFloat())
            putFloat("longitude", newSettings.longitude.toFloat())
            putString("timezone_id", newSettings.timeZoneId)
            putString("method_id", newSettings.method.id)
            putString("theme_mode", newSettings.themeMode.name)

            newSettings.offsets.forEach { (key, value) ->
                putInt("offset_$key", value)
            }
            newSettings.notifications.forEach { (key, value) ->
                putString("notif_$key", value.name)
            }
            apply()
        }
        _settingsFlow.value = newSettings
        alarmScheduler.scheduleDailyAlarms(newSettings)
    }

    suspend fun togglePrayerCompletion(dateString: String, prayerKey: String, isCompleted: Boolean) {
        if (isCompleted) {
            val log = PrayerLogEntity(
                dateString = dateString,
                prayerKey = prayerKey,
                isCompleted = true,
                completedAtMillis = System.currentTimeMillis()
            )
            dao.insertLog(log)
        } else {
            dao.deleteLog(dateString, prayerKey)
        }
    }

    suspend fun saveTasbihRecord(dhikrName: String, count: Int, targetCount: Int) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dao.insertTasbihRecord(
            TasbihRecordEntity(
                dhikrName = dhikrName,
                count = count,
                targetCount = targetCount,
                dateString = todayStr
            )
        )
    }

    fun syncAlarms() {
        alarmScheduler.scheduleDailyAlarms(_settingsFlow.value)
    }

    private fun calculateStreakMetrics(logs: List<PrayerLogEntity>): StreakMetrics {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCalendar = Calendar.getInstance()
        val todayStr = sdf.format(todayCalendar.time)

        val completedByDate = logs.filter { it.isCompleted }.groupBy { it.dateString }

        val obligatoryKeys = PrayerType.OBLIGATORY.map { it.key }.toSet()

        // Check if day is fully completed (all 5 obligatory prayers)
        fun isFullyCompleted(dateStr: String): Boolean {
            val dateLogs = completedByDate[dateStr] ?: return false
            val completedKeys = dateLogs.map { it.prayerKey }.toSet()
            return obligatoryKeys.all { completedKeys.contains(it) }
        }

        val totalPrayersCompleted = logs.count { it.isCompleted }

        val todayLogs = completedByDate[todayStr] ?: emptyList()
        val todayCompletedCount = todayLogs.count { obligatoryKeys.contains(it.prayerKey) && it.isCompleted }
        val isTodayFullyCompleted = isFullyCompleted(todayStr)

        // Calculate Current Streak
        var currentStreak = 0
        val checkCalendar = Calendar.getInstance()

        // If today is fully completed, start from today. Otherwise, check yesterday.
        if (isTodayFullyCompleted) {
            currentStreak++
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // If today is not fully completed yet, check yesterday to keep active streak
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val checkDateStr = sdf.format(checkCalendar.time)
            if (isFullyCompleted(checkDateStr)) {
                currentStreak++
                checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        // Longest streak calculation
        var longestStreak = currentStreak
        var tempStreak = 0
        val allDates = completedByDate.keys.sorted()
        for (dateStr in allDates) {
            if (isFullyCompleted(dateStr)) {
                tempStreak++
                if (tempStreak > longestStreak) {
                    longestStreak = tempStreak
                }
            } else {
                tempStreak = 0
            }
        }

        // Last 30 days history
        val last30Days = mutableListOf<Pair<String, Boolean>>()
        val dayCalendar = Calendar.getInstance()
        for (i in 0 until 30) {
            val dateStr = sdf.format(dayCalendar.time)
            val done = isFullyCompleted(dateStr)
            last30Days.add(Pair(dateStr, done))
            dayCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return StreakMetrics(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            totalPrayersCompleted = totalPrayersCompleted,
            todayCompletedCount = todayCompletedCount,
            totalObligatoryToday = 5,
            isTodayFullyCompleted = isTodayFullyCompleted,
            last30DaysStatus = last30Days
        )
    }
}
