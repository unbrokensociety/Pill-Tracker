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
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val medicationName = intent?.getStringExtra("EXTRA_MEDICATION_NAME") ?: context.getString(R.string.alarm_default_med)
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
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = context.getString(R.string.alarm_channel_desc)
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            // .setSmallIcon(R.mipmap.ic_launcher) // In real app, standard drawable
            // Using system icon for simplicity if resources not synced
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.alarm_title, medicationName))
            .setContentText(context.getString(R.string.alarm_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(scheduleId, builder.build())
    }
}
