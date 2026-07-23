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
    val endDate: Long? = null,
    val stockCount: Int = 30,
    val lowStockThreshold: Int = 5
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
    val timeMinute: Int,
    val sideEffectNote: String = ""
)

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey val email: String,
    val name: String,
    val authProvider: String, // "GOOGLE", "EMAIL", "GUEST"
    val avatarUrl: String = "",
    val passwordHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)


