package com.example.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.locale.LocaleHelper
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val localizedContext = LocaleHelper.getLocalizedContext(context)
        val medicationName = intent?.getStringExtra("EXTRA_MEDICATION_NAME") ?: localizedContext.getString(R.string.alarm_default_med)
        val scheduleId = intent?.getIntExtra("EXTRA_SCHEDULE_ID", -1) ?: -1
        
        showNotification(context, medicationName, scheduleId)

        if (scheduleId != -1) {
            val pendingResult = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context.applicationContext)
                    val dao = db.medicationDao()
                    val view = dao.getActiveScheduleViewByScheduleId(scheduleId)
                    if (view != null) {
                        val schedule = com.example.data.Schedule(
                            id = view.scheduleId,
                            medicationId = view.medicationId,
                            timeHour = view.timeHour,
                            timeMinute = view.timeMinute
                        )
                        val scheduler = AlarmScheduler(context.applicationContext)
                        scheduler.scheduleAlarm(schedule, view.name)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, medicationName: String, scheduleId: Int) {
        val localizedContext = LocaleHelper.getLocalizedContext(context)
        val notificationManager = localizedContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_channel"

        val alarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                localizedContext.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = localizedContext.getString(R.string.alarm_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmUri, audioAttributes)
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

        val builder = NotificationCompat.Builder(localizedContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(localizedContext.getString(R.string.alarm_title, finalMedName))
            .setContentText(localizedContext.getString(R.string.alarm_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(scheduleId, builder.build())
    }
}

