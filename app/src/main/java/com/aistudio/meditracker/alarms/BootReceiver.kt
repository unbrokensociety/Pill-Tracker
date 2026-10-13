package com.aistudio.meditracker.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistudio.meditracker.data.AppDatabase
import com.aistudio.meditracker.data.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.TIME_SET" ||
            action == "android.intent.action.TIMEZONE_CHANGED") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val dao = db.medicationDao()
                    val activeSchedules = dao.getAllActiveScheduleViews()
                    val scheduler = AlarmScheduler(context.applicationContext)

                    for (view in activeSchedules) {
                        val med = dao.getMedicationById(view.medicationId)
                        // Finished courses (end date passed or stock ran out)
                        // are not re-armed after a reboot / update.
                        if (med == null || com.aistudio.meditracker.data.MedicationRepository.isCourseFinished(med)) {
                            continue
                        }
                        val schedule = Schedule(
                            id = view.scheduleId,
                            medicationId = view.medicationId,
                            timeHour = view.timeHour,
                            timeMinute = view.timeMinute
                        )
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
}
