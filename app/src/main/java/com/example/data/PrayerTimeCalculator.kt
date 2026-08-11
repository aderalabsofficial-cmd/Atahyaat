package com.example.data

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class CalculatedPrayerTime(
    val type: PrayerType,
    val hour: Int,
    val minute: Int,
    val formattedTime: String,
    val timestampMillis: Long
)

class PrayerTimeCalculator {

    data class Location(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val timeZoneId: String
    )

    companion object {
        val DEFAULT_LOCATIONS = listOf(
            Location("Makkah, Saudi Arabia", 21.4225, 39.8262, "Asia/Riyadh"),
            Location("Jakarta, Indonesia", -6.2088, 106.8456, "Asia/Jakarta"),
            Location("London, United Kingdom", 51.5074, -0.1278, "Europe/London"),
            Location("New York, USA", 40.7128, -74.0060, "America/New_York"),
            Location("Cairo, Egypt", 30.0444, 31.2357, "Africa/Cairo"),
            Location("Karachi, Pakistan", 24.8607, 67.0011, "Asia/Karachi"),
            Location("Kuala Lumpur, Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
            Location("Istanbul, Turkey", 41.0082, 28.9784, "Europe/Istanbul"),
            Location("Dubai, UAE", 25.2048, 55.2708, "Asia/Dubai"),
            Location("Toronto, Canada", 43.6532, -79.3832, "America/Toronto")
        )
    }

    fun calculateTimes(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        method: PrayerScheduleMethod,
        customOffsets: Map<String, Int> = emptyMap()
    ): Map<PrayerType, CalculatedPrayerTime> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val tzOffsetHours = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0

        val julianDate = getJulianDate(year, month, day)
        val d = julianDate - 2451545.0

        // Sun parameters
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val L = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val declination = Math.toDegrees(asin(sin(Math.toRadians(e)) * sin(Math.toRadians(L))))

        // Equation of Time in hours
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(L)), cos(Math.toRadians(L)))) / 15.0
        val fixRa = fixHour(ra)
        val eqTime = q / 15.0 - fixRa

        val dhuhrDec = 12.0 + tzOffsetHours - longitude / 15.0 - eqTime

        // Sunrise & Sunset
        val sunriseHour = dhuhrDec - getSunHourAngle(latitude, declination, 0.8333) / 15.0
        val sunsetHour = dhuhrDec + getSunHourAngle(latitude, declination, 0.8333) / 15.0

        // Fajr
        val fajrHour = dhuhrDec - getSunHourAngle(latitude, declination, method.fajrAngle) / 15.0

        // Asr (Standard / Shafi'i)
        val asrAngle = Math.toDegrees(atan(1.0 + tan(Math.toRadians(abs(latitude - declination)))))
        val asrHour = dhuhrDec + getSunHourAngle(latitude, declination, 90.0 - asrAngle) / 15.0

        // Maghrib
        val maghribHour = sunsetHour

        // Isha
        val ishaHour = if (method.ishaIntervalMinutes > 0) {
            maghribHour + (method.ishaIntervalMinutes / 60.0)
        } else {
            dhuhrDec + getSunHourAngle(latitude, declination, method.ishaAngle) / 15.0
        }

        // Duha (approx 20 mins after sunrise)
        val duhaHour = sunriseHour + (20.0 / 60.0)

        // Tahajjud (last third of night, roughly 2 hours before Fajr)
        val tahajjudHour = fajrHour - 2.0

        val rawTimes = mapOf(
            PrayerType.FAJR to fajrHour,
            PrayerType.SUNRISE to sunriseHour,
            PrayerType.DUHA to duhaHour,
            PrayerType.DHUHR to dhuhrDec,
            PrayerType.ASR to asrHour,
            PrayerType.MAGHRIB to maghribHour,
            PrayerType.ISHA to ishaHour,
            PrayerType.TAHAJJUD to tahajjudHour
        )

        val resultMap = mutableMapOf<PrayerType, CalculatedPrayerTime>()

        for ((type, rawHour) in rawTimes) {
            val offsetMinutes = customOffsets[type.key] ?: 0
            val adjustedHour = rawHour + (offsetMinutes / 60.0)

            var h = adjustedHour.toInt()
            var m = ((adjustedHour - h) * 60).toInt()

            if (m >= 60) {
                h += 1
                m -= 60
            } else if (m < 0) {
                h -= 1
                m += 60
            }

            h = (h % 24 + 24) % 24

            val calcCalendar = calendar.clone() as Calendar
            calcCalendar.set(Calendar.HOUR_OF_DAY, h)
            calcCalendar.set(Calendar.MINUTE, m)
            calcCalendar.set(Calendar.SECOND, 0)
            calcCalendar.set(Calendar.MILLISECOND, 0)

            val formatted = formatTime(h, m)

            resultMap[type] = CalculatedPrayerTime(
                type = type,
                hour = h,
                minute = m,
                formattedTime = formatted,
                timestampMillis = calcCalendar.timeInMillis
            )
        }

        return resultMap
    }

    private fun getJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun getSunHourAngle(lat: Double, dec: Double, angle: Double): Double {
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(dec)
        val angleRad = Math.toRadians(angle)

        val cosH = (cos(Math.toRadians(90.0 + angle)) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))

        return when {
            cosH >= 1.0 -> 0.0
            cosH <= -1.0 -> 180.0
            else -> Math.toDegrees(acos(cosH))
        }
    }

    private fun fixAngle(a: Double): Double {
        var angle = a % 360.0
        if (angle < 0) angle += 360.0
        return angle
    }

    private fun fixHour(h: Double): Double {
        var hour = h % 24.0
        if (hour < 0) hour += 24.0
        return hour
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val ampm = if (hour >= 12) "PM" else "AM"
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return String.format("%d:%02d %s", h12, minute, ampm)
    }
}
