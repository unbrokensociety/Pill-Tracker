package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val notes: String = "",
    val color: Int = 0,
    val timesPerDay: Int = 1,
    val startDate: Long,
    val endDate: Long? = null
)

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val timeHour: Int,
    val timeMinute: Int
)

@Entity(tableName = "intake_logs")
data class IntakeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scheduleId: Int,
    val medicationId: Int,
    val timestampTaken: Long, // the actual epoch time it was taken
    val scheduledDateEpoch: Long, // representing the date it was scheduled for (start of day)
    val name: String,
    val dosage: String,
    val timeHour: Int,
    val timeMinute: Int
)
