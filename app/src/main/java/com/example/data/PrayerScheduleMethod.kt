package com.example.data

enum class PrayerScheduleMethod(
    val id: String,
    val title: String,
    val description: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMinutes: Int = 0
) {
    MWL("mwl", "Muslim World League", "Standard in Europe, Americas, parts of Asia", 18.0, 17.0),
    ISNA("isna", "ISNA (North America)", "Standard in USA and Canada", 15.0, 15.0),
    EGYPTIAN("egyptian", "Egyptian General Authority", "Standard in Egypt, Africa, Syria, Lebanon", 19.5, 17.5),
    UMM_AL_QURA("umm_al_qura", "Umm Al-Qura (Makkah)", "Standard in Saudi Arabia & Gulf region", 18.5, 0.0, ishaIntervalMinutes = 90),
    KARACHI("karachi", "Univ. of Islamic Sciences, Karachi", "Standard in Pakistan, India, Bangladesh", 18.0, 18.0),
    DUBAI("dubai", "Dubai / UAE Official", "Standard in UAE and neighboring countries", 18.2, 18.2),
    CUSTOM("custom", "Custom Schedule", "User defined angles and offsets", 18.0, 18.0);

    companion object {
        fun fromId(id: String): PrayerScheduleMethod {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: MWL
        }
    }
}
