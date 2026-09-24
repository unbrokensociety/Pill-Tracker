package com.aistudio.meditracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class MedicationRepository(private val dao: MedicationDao) {
    companion object {
        /**
         * A course is FINISHED when the supply is exhausted (stock tracking
         * on and the count hit zero) or the scheduled end date has passed.
         * Finished medications stop reminding and move to the "Finished"
         * group in the list — but their intake history always stays.
         */
        fun isCourseFinished(med: Medication): Boolean {
            val todayStart = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endPassed = med.endDate != null && med.endDate > 0L && med.endDate < todayStart
            val stockOut = med.trackStock && med.stockCount <= 0
            return endPassed || stockOut
        }
    }

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

    suspend fun getAllMedicationsOnce(): List<Medication> {
        return dao.getAllMedicationsOnce()
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
        // History is never wiped: intake logs carry denormalized name/dosage
        // copies, so past intakes remain in the calendar even after the
        // medication itself (and its schedules) is removed.
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
        val todayStart = getStartOfDayEpochMillis(LocalDate.now())
        return dao.getDailySchedules(dateEpoch, todayStart).map { list ->
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
