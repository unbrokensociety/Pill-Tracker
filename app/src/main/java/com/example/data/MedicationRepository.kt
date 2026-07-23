package com.example.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class MedicationRepository(private val dao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = dao.getAllMedications()
    val allIntakeLogDates: Flow<List<Long>> = dao.getAllIntakeLogDates()

    suspend fun addMedicationWithSchedules(medication: Medication, times: List<Pair<Int, Int>>): List<Schedule> {
        val medId = dao.insertMedication(medication).toInt()
        val schedulesToInsert = times.map { (hour, minute) ->
            Schedule(medicationId = medId, timeHour = hour, timeMinute = minute)
        }
        dao.insertSchedules(schedulesToInsert)
        return dao.getSchedulesForMedication(medId)
    }

    suspend fun getSchedulesForMedication(medicationId: Int): List<Schedule> {
        return dao.getSchedulesForMedication(medicationId)
    }

    suspend fun deleteMedication(medication: Medication) {
        dao.deleteSchedulesForMedication(medication.id)
        dao.deleteMedication(medication)
    }

    private fun getStartOfDayEpochMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getDailySchedules(date: LocalDate): Flow<List<DailyScheduleView>> {
        val dateEpoch = getStartOfDayEpochMillis(date)
        return dao.getDailySchedules(dateEpoch)
    }

    suspend fun getIntakeLog(scheduleId: Int, date: LocalDate): IntakeLog? {
        val dateEpoch = getStartOfDayEpochMillis(date)
        return dao.getIntakeLog(scheduleId, dateEpoch)
    }

    fun getIntakeLogsForDate(date: LocalDate): Flow<List<IntakeLog>> {
        val dateEpoch = getStartOfDayEpochMillis(date)
        return dao.getIntakeLogsForDate(dateEpoch)
    }

    val lowStockMedications: Flow<List<Medication>> = dao.getLowStockMedications()

    suspend fun refillStock(medicationId: Int, amount: Int) {
        dao.refillStock(medicationId, amount)
    }

    suspend fun toggleIntake(schedule: DailyScheduleView, date: LocalDate, isTaken: Boolean, sideEffectNote: String = "") {
        val dateEpoch = getStartOfDayEpochMillis(date)
        if (isTaken) {
            val log = IntakeLog(
                scheduleId = schedule.scheduleId,
                medicationId = schedule.medicationId,
                timestampTaken = System.currentTimeMillis(),
                scheduledDateEpoch = dateEpoch,
                name = schedule.name,
                dosage = schedule.dosage,
                timeHour = schedule.timeHour,
                timeMinute = schedule.timeMinute,
                sideEffectNote = sideEffectNote
            )
            dao.insertIntakeLog(log)
            dao.decrementStock(schedule.medicationId)
        } else {
            dao.deleteIntakeLog(schedule.scheduleId, dateEpoch)
            dao.incrementStock(schedule.medicationId)
        }
    }
}
