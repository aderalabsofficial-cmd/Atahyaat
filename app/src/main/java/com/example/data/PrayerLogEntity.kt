package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_logs")
data class PrayerLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String, // e.g. "2026-08-11"
    val prayerKey: String,   // e.g. "fajr", "dhuhr", "asr", "maghrib", "isha"
    val isCompleted: Boolean,
    val completedAtMillis: Long = System.currentTimeMillis(),
    val isOnTime: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "tasbih_records")
data class TasbihRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dhikrName: String,
    val count: Int,
    val targetCount: Int,
    val dateString: String,
    val completedAtMillis: Long = System.currentTimeMillis()
)
