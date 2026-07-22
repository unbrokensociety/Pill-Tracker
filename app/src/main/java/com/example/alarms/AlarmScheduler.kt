package com.example.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(schedule: Schedule, medicationName: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_SCHEDULE_ID", schedule.id)
            putExtra("EXTRA_MEDICATION_ID", schedule.medicationId)
            putExtra("EXTRA_MEDICATION_NAME", medicationName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Find the next time it happens today or tomorrow
        val now = LocalDateTime.now()
        var nextTime = now.withHour(schedule.timeHour).withMinute(schedule.timeMinute).withSecond(0)
        
        if (nextTime.isBefore(now)) {
            nextTime = nextTime.plusDays(1)
        }

        val triggerAtMillis = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                // Extreme fallback
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAlarm(schedule: Schedule) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
