package com.aistudio.meditracker.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aistudio.meditracker.MainActivity
import com.aistudio.meditracker.R
import com.aistudio.meditracker.ui.locale.LocaleHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SNOOZE = "com.aistudio.meditracker.meditracker.ACTION_SNOOZE"
        const val ACTION_TAKEN = "com.aistudio.meditracker.meditracker.ACTION_TAKEN"
        const val EXTRA_SNOOZE_MINUTES = "EXTRA_SNOOZE_MINUTES"
        private const val CHANNEL_NORMAL = "medication_channel"
        private const val CHANNEL_CRITICAL = "medication_channel_critical"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val localizedContext = LocaleHelper.getLocalizedContext(context)
        val medicationName = intent?.getStringExtra("EXTRA_MEDICATION_NAME") ?: localizedContext.getString(R.string.alarm_default_med)
        val scheduleId = intent?.getIntExtra("EXTRA_SCHEDULE_ID", -1) ?: -1

        if (intent?.action == ACTION_SNOOZE) {
            val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (scheduleId != -1) {
                notificationManager.cancel(scheduleId)
            }

            val scheduler = AlarmScheduler(context.applicationContext)
            scheduler.scheduleSnooze(scheduleId, medicationName, snoozeMinutes)
            return
        }

        if (intent?.action == ACTION_TAKEN) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (scheduleId != -1) {
                notificationManager.cancel(scheduleId)
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (scheduleId != -1) {
                        val db = com.aistudio.meditracker.data.AppDatabase.getDatabase(context.applicationContext)
                        val dao = db.medicationDao()
                        val view = dao.getActiveScheduleViewByScheduleId(scheduleId)
                        if (view != null) {
                            val todayEpoch = LocalDate.now()
                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val alreadyLogged = dao.getIntakeLog(scheduleId, todayEpoch) != null
                            if (!alreadyLogged) {
                                val log = com.aistudio.meditracker.data.IntakeLog(
                                    scheduleId = view.scheduleId,
                                    medicationId = view.medicationId,
                                    timestampTaken = System.currentTimeMillis(),
                                    scheduledDateEpoch = todayEpoch,
                                    name = view.name,
                                    dosage = view.dosage,
                                    timeHour = view.timeHour,
                                    timeMinute = view.timeMinute
                                )
                                dao.insertIntakeLog(log)
                                dao.decrementStock(view.medicationId)
                            }
                        }
                        AlarmScheduler(context.applicationContext).cancelSnoozeAlarms(scheduleId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepo = com.aistudio.meditracker.data.SettingsRepository(context.applicationContext)
                val isNotifEnabled = settingsRepo.notificationsFlow.first()
                val isPersistent = settingsRepo.persistentReminderFlow.first()
                val isCritical = settingsRepo.criticalAlertsFlow.first()
                val isAlarmMode = settingsRepo.alarmModeFlow.first()

                var alreadyTakenToday = false

                if (scheduleId != -1) {
                    val db = com.aistudio.meditracker.data.AppDatabase.getDatabase(context.applicationContext)
                    val dao = db.medicationDao()
                    val view = dao.getActiveScheduleViewByScheduleId(scheduleId)
                    val todayEpoch = LocalDate.now()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    alreadyTakenToday = dao.getIntakeLog(scheduleId, todayEpoch) != null

                    // Reschedule the next occurrence while the course is active.
                    if (view != null) {
                        val med = dao.getMedicationById(view.medicationId)
                        val now = LocalDateTime.now()
                        var nextTime = now.withHour(view.timeHour).withMinute(view.timeMinute).withSecond(0).withNano(0)
                        if (nextTime.isBefore(now.minusSeconds(5))) {
                            nextTime = nextTime.plusDays(1)
                        }
                        val nextTimeMillis = nextTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                        if (med == null || (med.endDate != null && med.endDate > 0L && nextTimeMillis > med.endDate)) {
                            // Medication expired or removed, do not reschedule
                        } else {
                            val schedule = com.aistudio.meditracker.data.Schedule(
                                id = view.scheduleId,
                                medicationId = view.medicationId,
                                timeHour = view.timeHour,
                                timeMinute = view.timeMinute
                            )
                            val scheduler = AlarmScheduler(context.applicationContext)
                            scheduler.scheduleAlarm(schedule, view.name)
                        }
                    }
                }

                if (isNotifEnabled && !alreadyTakenToday) {
                    showNotification(context, medicationName, scheduleId, isPersistent, isCritical, isAlarmMode)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        medicationName: String,
        scheduleId: Int,
        persistent: Boolean,
        critical: Boolean,
        alarmMode: Boolean
    ) {
        val localizedContext = LocaleHelper.getLocalizedContext(context)
        val notificationManager = localizedContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        val audioAttributes = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .build()

        // The critical channel bypasses Do Not Disturb; the bypass only takes
        // effect while the app holds notification policy access.
        val policyGranted = try {
            notificationManager.isNotificationPolicyAccessGranted
        } catch (_: Exception) {
            false
        }
        val channelId = if (critical && policyGranted) CHANNEL_CRITICAL else CHANNEL_NORMAL

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                localizedContext.getString(
                    if (channelId == CHANNEL_CRITICAL) R.string.alarm_channel_critical_name
                    else R.string.alarm_channel_name
                ),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = localizedContext.getString(
                    if (channelId == CHANNEL_CRITICAL) R.string.alarm_channel_critical_desc
                    else R.string.alarm_channel_desc
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmUri, audioAttributes)
                if (channelId == CHANNEL_CRITICAL) {
                    setBypassDnd(true)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(localizedContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            localizedContext,
            scheduleId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultMedName = localizedContext.getString(R.string.alarm_default_med)
        val finalMedName = if (medicationName.isBlank()) defaultMedName else medicationName

        // "Taken" action: logs the intake and clears the reminder.
        val takenIntent = Intent(localizedContext, AlarmReceiver::class.java).apply {
            action = ACTION_TAKEN
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_MEDICATION_NAME", finalMedName)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            localizedContext,
            scheduleId * 100 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 15 min intent
        val snooze15Intent = Intent(localizedContext, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_MEDICATION_NAME", finalMedName)
            putExtra(EXTRA_SNOOZE_MINUTES, 15)
        }
        val snooze15PendingIntent = PendingIntent.getBroadcast(
            localizedContext,
            scheduleId * 100 + 15,
            snooze15Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 30 min intent
        val snooze30Intent = Intent(localizedContext, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_MEDICATION_NAME", finalMedName)
            putExtra(EXTRA_SNOOZE_MINUTES, 30)
        }
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            localizedContext,
            scheduleId * 100 + 30,
            snooze30Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(localizedContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(localizedContext.getString(R.string.alarm_title, finalMedName))
            .setContentText(localizedContext.getString(R.string.alarm_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .addAction(0, localizedContext.getString(R.string.notif_taken), takenPendingIntent)
            .addAction(0, localizedContext.getString(R.string.snooze_15m), snooze15PendingIntent)
            .addAction(0, localizedContext.getString(R.string.snooze_30m), snooze30PendingIntent)

        if (persistent) {
            // Pinned "island" reminder: it survives in the shade until the
            // dose is taken (the Taken action or an in-app log clears it).
            builder.setOngoing(true)
            builder.setAutoCancel(false)
        } else {
            builder.setAutoCancel(true)
        }

        if (alarmMode) {
            // Alarm-clock presentation: wake the screen and ring until answered.
            val fullScreenIntent = Intent(localizedContext, AlarmFullScreenActivity::class.java).apply {
                putExtra("EXTRA_SCHEDULE_ID", scheduleId)
                putExtra("EXTRA_MEDICATION_NAME", finalMedName)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                localizedContext,
                scheduleId * 100 + 2,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        notificationManager.notify(scheduleId, builder.build())
    }
}
