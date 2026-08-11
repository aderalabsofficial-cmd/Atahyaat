package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector

enum class PrayerType(
    val key: String,
    val displayName: String,
    val arabicName: String,
    val isObligatory: Boolean,
    val defaultIcon: ImageVector,
    val description: String
) {
    FAJR("fajr", "Fajr", "الفجر", true, Icons.Default.WbTwilight, "Dawn prayer before sunrise"),
    SUNRISE("sunrise", "Sunrise", "الشروق", false, Icons.Default.WbCloudy, "Sunrise time (prayer prohibited)"),
    DUHA("duha", "Duha", "الضحى", false, Icons.Default.WbSunny, "Optional morning prayer"),
    DHUHR("dhuhr", "Dhuhr", "الظهر", true, Icons.Default.Brightness7, "Midday prayer after zenith"),
    ASR("asr", "Asr", "العصر", true, Icons.Default.Brightness5, "Afternoon prayer"),
    MAGHRIB("maghrib", "Maghrib", "المغرب", true, Icons.Default.Brightness2, "Sunset prayer immediately after dusk"),
    ISHA("isha", "Isha", "العشاء", true, Icons.Default.NightlightRound, "Night prayer after twilight"),
    TAHAJJUD("tahajjud", "Tahajjud", "Tahajjud", false, Icons.Default.Bedtime, "Voluntary late night prayer");

    companion object {
        val OBLIGATORY = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
        val ALL_DAILY = listOf(FAJR, SUNRISE, DUHA, DHUHR, ASR, MAGHRIB, ISHA, TAHAJJUD)

        fun fromKey(key: String): PrayerType {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: FAJR
        }
    }
}
