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
        if (schedule.timeHour < 0 || schedule.timeMinute < 0) {
            return
        }

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

        val now = LocalDateTime.now()
        var nextTime = now.withHour(schedule.timeHour).withMinute(schedule.timeMinute).withSecond(0).withNano(0)
        
        // If time today has passed (more than 5 seconds ago), schedule for tomorrow
        if (nextTime.isBefore(now.minusSeconds(5))) {
            nextTime = nextTime.plusDays(1)
        }

        val triggerAtMillis = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: SecurityException) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun scheduleSnooze(scheduleId: Int, medicationName: String, snoozeMinutes: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_MEDICATION_NAME", medicationName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId * 1000 + snoozeMinutes,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                alarmManager.setAndAllowWhileIdle(
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
