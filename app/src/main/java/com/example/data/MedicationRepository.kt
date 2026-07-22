package com.example.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneOffset

class MedicationRepository(private val dao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = dao.getAllMedications()

    suspend fun addMedicationWithSchedules(medication: Medication, times: List<Pair<Int, Int>>) {
        val medId = dao.insertMedication(medication).toInt()
        val schedules = times.map { (hour, minute) ->
            Schedule(medicationId = medId, timeHour = hour, timeMinute = minute)
        }
        dao.insertSchedules(schedules)
    }

    suspend fun deleteMedication(medication: Medication) {
        dao.deleteSchedulesForMedication(medication.id)
        dao.deleteMedication(medication)
    }

    fun getDailySchedules(date: LocalDate): Flow<List<DailyScheduleView>> {
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        return dao.getDailySchedules(dateEpoch * 1000)
    }

    suspend fun getIntakeLog(scheduleId: Int, date: LocalDate): IntakeLog? {
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        return dao.getIntakeLog(scheduleId, dateEpoch)
    }

    fun getIntakeLogsForDate(date: LocalDate): Flow<List<IntakeLog>> {
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        return dao.getIntakeLogsForDate(dateEpoch)
    }

    suspend fun toggleIntake(schedule: DailyScheduleView, date: LocalDate, isTaken: Boolean) {
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        if (isTaken) {
            val log = IntakeLog(
                scheduleId = schedule.scheduleId,
                medicationId = schedule.medicationId,
                timestampTaken = System.currentTimeMillis(),
                scheduledDateEpoch = dateEpoch,
                name = schedule.name,
                dosage = schedule.dosage,
                timeHour = schedule.timeHour,
                timeMinute = schedule.timeMinute
            )
            dao.insertIntakeLog(log)
        } else {
            dao.deleteIntakeLog(schedule.scheduleId, dateEpoch)
        }
    }
}
