package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {

    @Query("SELECT * FROM prayer_logs WHERE dateString = :dateString")
    fun getLogsForDate(dateString: String): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_logs WHERE dateString = :dateString AND prayerKey = :prayerKey LIMIT 1")
    suspend fun getLog(dateString: String, prayerKey: String): PrayerLogEntity?

    @Query("SELECT * FROM prayer_logs ORDER BY dateString DESC, completedAtMillis DESC")
    fun getAllLogsFlow(): Flow<List<PrayerLogEntity>>

    @Query("SELECT DISTINCT dateString FROM prayer_logs WHERE isCompleted = 1 ORDER BY dateString DESC")
    fun getCompletedDatesFlow(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM prayer_logs WHERE isCompleted = 1")
    fun getTotalCompletedCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: PrayerLogEntity)

    @Query("DELETE FROM prayer_logs WHERE dateString = :dateString AND prayerKey = :prayerKey")
    suspend fun deleteLog(dateString: String, prayerKey: String)

    @Query("SELECT * FROM tasbih_records ORDER BY completedAtMillis DESC")
    fun getTasbihRecordsFlow(): Flow<List<TasbihRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbihRecord(record: TasbihRecordEntity)
}
