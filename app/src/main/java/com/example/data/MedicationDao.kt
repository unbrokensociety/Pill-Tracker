package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Int): Medication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<Schedule>)

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    suspend fun getSchedulesForMedication(medicationId: Int): List<Schedule>

    @Query("DELETE FROM schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedulesForMedication(medicationId: Int)

    // A unified query to get today's schedules with medication info
    @Query("""
        SELECT s.id as scheduleId, m.id as medicationId, m.name, m.dosage, m.color, s.timeHour, s.timeMinute 
        FROM medications m 
        INNER JOIN schedules s ON m.id = s.medicationId
        WHERE :date >= m.startDate AND (m.endDate IS NULL OR :date <= m.endDate)
        ORDER BY s.timeHour, s.timeMinute
    """)
    fun getDailySchedules(date: Long): Flow<List<DailyScheduleView>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakeLog(log: IntakeLog)

    @Query("SELECT * FROM intake_logs WHERE scheduledDateEpoch = :dateEpoch")
    fun getIntakeLogsForDate(dateEpoch: Long): Flow<List<IntakeLog>>

    @Query("DELETE FROM intake_logs WHERE scheduleId = :scheduleId AND scheduledDateEpoch = :dateEpoch")
    suspend fun deleteIntakeLog(scheduleId: Int, dateEpoch: Long)
    
    @Query("SELECT * FROM intake_logs WHERE scheduleId = :scheduleId AND scheduledDateEpoch = :dateEpoch LIMIT 1")
    suspend fun getIntakeLog(scheduleId: Int, dateEpoch: Long): IntakeLog?

    @Query("SELECT DISTINCT scheduledDateEpoch FROM intake_logs ORDER BY scheduledDateEpoch DESC")
    fun getAllIntakeLogDates(): Flow<List<Long>>

    @Query("""
        SELECT s.id as scheduleId, m.id as medicationId, m.name, m.dosage, m.color, s.timeHour, s.timeMinute 
        FROM medications m 
        INNER JOIN schedules s ON m.id = s.medicationId
    """)
    suspend fun getAllActiveScheduleViews(): List<DailyScheduleView>

    @Query("""
        SELECT s.id as scheduleId, m.id as medicationId, m.name, m.dosage, m.color, s.timeHour, s.timeMinute 
        FROM medications m 
        INNER JOIN schedules s ON m.id = s.medicationId
        WHERE s.id = :scheduleId LIMIT 1
    """)
    suspend fun getActiveScheduleViewByScheduleId(scheduleId: Int): DailyScheduleView?
}

data class DailyScheduleView(
    val scheduleId: Int,
    val medicationId: Int,
    val name: String,
    val dosage: String,
    val color: Int,
    val timeHour: Int,
    val timeMinute: Int
)
