package com.aistudio.meditracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class MedicationRepository(private val dao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = dao.getAllMedications()
    val allIntakeLogDates: Flow<List<Long>> = dao.getAllIntakeLogDates()

    suspend fun addMedicationWithSchedules(medication: Medication, times: List<Pair<Int, Int>>): List<Schedule> {
        val medId = dao.insertMedication(medication).toInt()
        val effectiveTimes = if (medication.scheduleType == "as_needed" || times.isEmpty()) {
            listOf(-1 to -1)
        } else {
            times.distinct().sortedBy { (hour, minute) -> hour * 60 + minute }
        }
        val schedulesToInsert = effectiveTimes.map { (hour, minute) ->
            Schedule(medicationId = medId, timeHour = hour, timeMinute = minute)
        }
        dao.insertSchedules(schedulesToInsert)
        return dao.getSchedulesForMedication(medId)
    }

    suspend fun getMedicationById(id: Int): Medication? {
        return dao.getMedicationById(id)
    }

    suspend fun updateMedicationWithSchedules(medication: Medication, times: List<Pair<Int, Int>>): List<Schedule> {
        dao.insertMedication(medication)
        dao.deleteSchedulesForMedication(medication.id)
        val effectiveTimes = if (medication.scheduleType == "as_needed" || times.isEmpty()) {
            listOf(-1 to -1)
        } else {
            times.distinct().sortedBy { (hour, minute) -> hour * 60 + minute }
        }
        val schedulesToInsert = effectiveTimes.map { (hour, minute) ->
            Schedule(medicationId = medication.id, timeHour = hour, timeMinute = minute)
        }
        dao.insertSchedules(schedulesToInsert)
        val newSchedules = dao.getSchedulesForMedication(medication.id)

        // BUG FIX: schedule rows are recreated with new IDs on every edit, which used to
        // orphan today's "taken" state and history. Re-attach the logs to the fresh rows
        // by matching medication + intake time so state & history survive edits.
        newSchedules.forEach { schedule ->
            dao.reassignIntakeLogs(
                medicationId = medication.id,
                timeHour = schedule.timeHour,
                timeMinute = schedule.timeMinute,
                newScheduleId = schedule.id
            )
        }

        return newSchedules
    }

    suspend fun getSchedulesForMedication(medicationId: Int): List<Schedule> {
        return dao.getSchedulesForMedication(medicationId)
    }

    suspend fun deleteMedication(medication: Medication) {
        dao.deleteIntakeLogsForMedication(medication.id)
        dao.deleteSchedulesForMedication(medication.id)
        dao.deleteMedication(medication)
    }

    suspend fun clearAllData() {
        dao.deleteAllIntakeLogs()
        dao.deleteAllSchedules()
        dao.deleteAllMedications()
    }

    suspend fun getAllActiveScheduleViews(): List<DailyScheduleView> {
        return dao.getAllActiveScheduleViews()
    }

    private fun getStartOfDayEpochMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getDailySchedules(date: LocalDate): Flow<List<DailyScheduleView>> {
        val dateEpoch = getStartOfDayEpochMillis(date)
        return dao.getDailySchedules(dateEpoch).map { list ->
            list.filter { item ->
                if (item.scheduleType == "interval") {
                    val startLocal = java.time.Instant.ofEpochMilli(item.startDate)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    if (date.isBefore(startLocal)) {
                        false
                    } else {
                        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startLocal, date)
                        val interval = if (item.intervalDays > 0) item.intervalDays else 1
                        daysDiff >= 0 && (daysDiff % interval == 0L)
                    }
                } else {
                    true
                }
            }
        }
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
