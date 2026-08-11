package com.example.data

enum class DarkThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Midnight Emerald (Dark)");

    companion object {
        fun fromName(name: String): DarkThemeMode {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
        }
    }
}

enum class AlarmNotificationType(val title: String) {
    SOUND_AND_NOTIFICATION("Adhan / Alarm Sound & Notification"),
    NOTIFICATION_ONLY("Silent Notification"),
    OFF("Disabled");

    companion object {
        fun fromName(name: String): AlarmNotificationType {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: SOUND_AND_NOTIFICATION
        }
    }
}

data class PrayerSettings(
    val locationName: String = "Makkah, Saudi Arabia",
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val timeZoneId: String = "Asia/Riyadh",
    val method: PrayerScheduleMethod = PrayerScheduleMethod.UMM_AL_QURA,
    val themeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    val prePrayerReminderMinutes: Int = 10,
    val offsets: Map<String, Int> = mapOf(
        "fajr" to 0,
        "dhuhr" to 0,
        "asr" to 0,
        "maghrib" to 0,
        "isha" to 0
    ),
    val notifications: Map<String, AlarmNotificationType> = mapOf(
        "fajr" to AlarmNotificationType.SOUND_AND_NOTIFICATION,
        "dhuhr" to AlarmNotificationType.SOUND_AND_NOTIFICATION,
        "asr" to AlarmNotificationType.SOUND_AND_NOTIFICATION,
        "maghrib" to AlarmNotificationType.SOUND_AND_NOTIFICATION,
        "isha" to AlarmNotificationType.SOUND_AND_NOTIFICATION,
        "tahajjud" to AlarmNotificationType.NOTIFICATION_ONLY,
        "duha" to AlarmNotificationType.NOTIFICATION_ONLY
    )
)
