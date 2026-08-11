package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmNotificationType
import com.example.data.CalculatedPrayerTime
import com.example.data.DarkThemeMode
import com.example.data.PrayerLogEntity
import com.example.data.PrayerRepository
import com.example.data.PrayerScheduleMethod
import com.example.data.PrayerSettings
import com.example.data.PrayerTimeCalculator
import com.example.data.PrayerType
import com.example.data.StreakMetrics
import com.example.data.TasbihRecordEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NextPrayerInfo(
    val type: PrayerType,
    val formattedTime: String,
    val timestampMillis: Long,
    val timeRemainingString: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = PrayerRepository(application)
    private val calculator = PrayerTimeCalculator()

    val settings: StateFlow<PrayerSettings> = repository.settingsFlow

    private val _selectedDateString = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDateString: StateFlow<String> = _selectedDateString.asStateFlow()

    // Logs for selected date
    val selectedDateLogs: StateFlow<List<PrayerLogEntity>> = _selectedDateString.flatMapLatest { dateStr ->
        repository.getLogsForDateFlow(dateStr)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val streakMetrics: StateFlow<StreakMetrics> = repository.streakMetricsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakMetrics()
    )

    val tasbihHistory: StateFlow<List<TasbihRecordEntity>> = repository.tasbihRecordsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Daily prayer times map for currently selected date
    private val _dailyPrayerTimes = MutableStateFlow<Map<PrayerType, CalculatedPrayerTime>>(emptyMap())
    val dailyPrayerTimes: StateFlow<Map<PrayerType, CalculatedPrayerTime>> = _dailyPrayerTimes.asStateFlow()

    // Next Prayer Countdown Info
    private val _nextPrayerInfo = MutableStateFlow<NextPrayerInfo?>(null)
    val nextPrayerInfo: StateFlow<NextPrayerInfo?> = _nextPrayerInfo.asStateFlow()

    // Digital Tasbih State
    private val _dhikrTitle = MutableStateFlow("SubhanAllah (سبحان الله)")
    val dhikrTitle: StateFlow<String> = _dhikrTitle.asStateFlow()

    private val _tasbihCount = MutableStateFlow(0)
    val tasbihCount: StateFlow<Int> = _tasbihCount.asStateFlow()

    private val _tasbihTarget = MutableStateFlow(33)
    val tasbihTarget: StateFlow<Int> = _tasbihTarget.asStateFlow()

    init {
        // Recompute prayer times whenever settings or selected date change
        viewModelScope.launch {
            combine(settings, _selectedDateString) { s, dateStr ->
                computePrayerTimesForDate(s, dateStr)
            }.collectLatest { timesMap ->
                _dailyPrayerTimes.value = timesMap
            }
        }

        // Start periodic timer for next prayer countdown
        viewModelScope.launch {
            while (true) {
                updateNextPrayerInfo()
                delay(1000)
            }
        }

        // Sync alarms on launch
        repository.syncAlarms()
    }

    private fun computePrayerTimesForDate(
        settings: PrayerSettings,
        dateStr: String
    ): Map<PrayerType, CalculatedPrayerTime> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        try {
            val date = sdf.parse(dateStr)
            if (date != null) {
                cal.time = date
            }
        } catch (e: Exception) {
            // fallback
        }

        return calculator.calculateTimes(
            calendar = cal,
            latitude = settings.latitude,
            longitude = settings.longitude,
            method = settings.method,
            customOffsets = settings.offsets
        )
    }

    private fun updateNextPrayerInfo() {
        val currentSettings = settings.value
        val nowCal = Calendar.getInstance()
        val nowMillis = nowCal.timeInMillis

        val todayTimes = calculator.calculateTimes(
            calendar = nowCal,
            latitude = currentSettings.latitude,
            longitude = currentSettings.longitude,
            method = currentSettings.method,
            customOffsets = currentSettings.offsets
        )

        // Find next prayer today
        val nextToday = PrayerType.OBLIGATORY
            .mapNotNull { todayTimes[it] }
            .filter { it.timestampMillis > nowMillis }
            .minByOrNull { it.timestampMillis }

        if (nextToday != null) {
            val diffMs = nextToday.timestampMillis - nowMillis
            val hours = diffMs / (1000 * 60 * 60)
            val minutes = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = (diffMs % (1000 * 60)) / 1000

            val remainingStr = String.format("%02dh %02dm %02ds", hours, minutes, seconds)
            _nextPrayerInfo.value = NextPrayerInfo(
                type = nextToday.type,
                formattedTime = nextToday.formattedTime,
                timestampMillis = nextToday.timestampMillis,
                timeRemainingString = remainingStr
            )
        } else {
            // Tomorrow's Fajr
            val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowTimes = calculator.calculateTimes(
                calendar = tomorrowCal,
                latitude = currentSettings.latitude,
                longitude = currentSettings.longitude,
                method = currentSettings.method,
                customOffsets = currentSettings.offsets
            )
            val fajrTomorrow = tomorrowTimes[PrayerType.FAJR]
            if (fajrTomorrow != null) {
                val diffMs = fajrTomorrow.timestampMillis - nowMillis
                val hours = diffMs / (1000 * 60 * 60)
                val minutes = (diffMs % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (diffMs % (1000 * 60)) / 1000

                val remainingStr = String.format("%02dh %02dm %02ds", hours, minutes, seconds)
                _nextPrayerInfo.value = NextPrayerInfo(
                    type = PrayerType.FAJR,
                    formattedTime = fajrTomorrow.formattedTime,
                    timestampMillis = fajrTomorrow.timestampMillis,
                    timeRemainingString = remainingStr
                )
            }
        }
    }

    fun togglePrayerCompletion(prayerKey: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.togglePrayerCompletion(_selectedDateString.value, prayerKey, isCompleted)
        }
    }

    fun setSelectedDate(dateString: String) {
        _selectedDateString.value = dateString
    }

    fun setLocation(locationName: String, lat: Double, lng: Double, tzId: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(
                current.copy(
                    locationName = locationName,
                    latitude = lat,
                    longitude = lng,
                    timeZoneId = tzId
                )
            )
        }
    }

    fun setMethod(method: PrayerScheduleMethod) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(method = method))
        }
    }

    fun setThemeMode(mode: DarkThemeMode) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(themeMode = mode))
        }
    }

    fun updateOffset(prayerKey: String, offsetMinutes: Int) {
        viewModelScope.launch {
            val current = settings.value
            val newOffsets = current.offsets.toMutableMap().apply {
                put(prayerKey, offsetMinutes)
            }
            repository.updateSettings(current.copy(offsets = newOffsets))
        }
    }

    fun updateNotificationSetting(prayerKey: String, notifType: AlarmNotificationType) {
        viewModelScope.launch {
            val current = settings.value
            val newNotifs = current.notifications.toMutableMap().apply {
                put(prayerKey, notifType)
            }
            repository.updateSettings(current.copy(notifications = newNotifs))
        }
    }

    fun incrementTasbih() {
        val newCount = _tasbihCount.value + 1
        _tasbihCount.value = newCount
        if (newCount >= _tasbihTarget.value) {
            viewModelScope.launch {
                repository.saveTasbihRecord(_dhikrTitle.value, newCount, _tasbihTarget.value)
            }
        }
    }

    fun resetTasbih() {
        _tasbihCount.value = 0
    }

    fun selectDhikr(title: String, target: Int) {
        _dhikrTitle.value = title
        _tasbihTarget.value = target
        _tasbihCount.value = 0
    }
}
