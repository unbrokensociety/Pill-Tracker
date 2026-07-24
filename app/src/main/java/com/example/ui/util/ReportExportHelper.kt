package com.example.ui.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.DailyScheduleView
import com.example.data.IntakeLog
import com.example.data.Medication
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ReportExportHelper {

    fun generateAndShareReport(
        context: Context,
        userAccountName: String,
        userEmail: String,
        medications: List<Medication>,
        schedules: List<DailyScheduleView>,
        todayLogs: List<IntakeLog>,
        streakDays: Int
    ) {
        val currentLang = context.resources.configuration.locales[0].language
        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        val builder = StringBuilder()

        val totalScheduled = schedules.size
        val takenCount = todayLogs.size
        val adherenceRate = if (totalScheduled > 0) ((takenCount.toDouble() / totalScheduled.toDouble()) * 100).toInt() else 100

        val isUk = currentLang == "uk"

        val titleHeader = if (isUk) "МЕДИЧНИЙ ЗВІТ ПРИЙОМУ ЛІКІВ" else "CLINICAL & PHARMACOLOGICAL REPORT"
        val patientLabel = if (isUk) "Пацієнт" else "Patient Name"
        val dateLabel = if (isUk) "Дата формування" else "Report Timestamp"
        val emailLabel = if (isUk) "Електронна пошта" else "Account Email"
        val adherenceLabel = if (isUk) "Рівень виконання" else "Adherence Rate"
        val streakLabel = if (isUk) "Днів поспіль активності" else "Compliance Streak"

        builder.append("=====================================================\n")
        builder.append("         $titleHeader\n")
        builder.append("                    PILL TRACKER                     \n")
        builder.append("=====================================================\n")
        builder.append("$dateLabel : $dateStr\n")
        builder.append("$patientLabel     : ${userAccountName.ifBlank { if (isUk) "Гостьовий Пацієнт" else "Guest Patient" }}\n")
        if (userEmail.isNotBlank()) {
            builder.append("$emailLabel : $userEmail\n")
        }
        builder.append("$adherenceLabel   : $adherenceRate% ($takenCount/$totalScheduled прийомів за сьогодні)\n")
        builder.append("$streakLabel: $streakDays дн.\n\n")

        val sec1Title = if (isUk) "1. СПИСОК АКТИВНИХ ЛІКІВ (${medications.size})" else "1. PRESCRIPTION & STOCK REGISTRY (${medications.size} Active)"
        builder.append("-----------------------------------------------------\n")
        builder.append(" $sec1Title\n")
        builder.append("-----------------------------------------------------\n")
        if (medications.isEmpty()) {
            builder.append(if (isUk) "Активні ліки відсутні.\n\n" else "No active medications registered.\n\n")
        } else {
            medications.forEachIndexed { index, med ->
                val lowStockMsg = if (isUk) "⚠️ Потрібно поповнити запаси!" else "⚠️ LOW STOCK REFILL NEEDED!"
                val stockAlert = if (med.stockCount <= med.lowStockThreshold) lowStockMsg else "OK"
                builder.append("[${index + 1}] ${med.name.uppercase()} — ${med.dosage}\n")
                if (med.notes.isNotBlank()) builder.append("    ${if (isUk) "Примітки" else "Clinical Notes"}  : ${med.notes}\n")
                builder.append("    ${if (isUk) "Частота" else "Daily Frequency"} : ${med.timesPerDay} раз(ів) на день\n")
                builder.append("    ${if (isUk) "Залишок" else "Current Inventory"}: ${med.stockCount} шт. [$stockAlert]\n\n")
            }
        }

        val sec2Title = if (isUk) "2. РОЗКЛАД ТА СТАТУС ПРИЙОМУ НА СЬОГОДНІ" else "2. TODAY'S SCHEDULE & COMPLIANCE LOG"
        builder.append("-----------------------------------------------------\n")
        builder.append(" $sec2Title\n")
        builder.append("-----------------------------------------------------\n")
        if (schedules.isEmpty()) {
            builder.append(if (isUk) "На сьогодні прийомів не заплановано.\n\n" else "No medication doses scheduled for today.\n\n")
        } else {
            schedules.forEach { sch ->
                val matchingLog = todayLogs.find { it.scheduleId == sch.scheduleId }
                val isTaken = matchingLog != null
                val timeFormatted = String.format("%02d:%02d", sch.timeHour, sch.timeMinute)
                val statusSymbol = if (isTaken) (if (isUk) "✓ ПРИЙНЯТО" else "✓ TAKEN") else (if (isUk) "✗ ОЧІКУЄТЬСЯ" else "✗ PENDING")
                
                builder.append("• $timeFormatted - ${sch.name} (${sch.dosage}) -> $statusSymbol")
                if (matchingLog != null && matchingLog.sideEffectNote.isNotBlank()) {
                    builder.append(" [${if (isUk) "Побічний ефект" else "Side Effect"}: ${matchingLog.sideEffectNote}]")
                }
                builder.append("\n")
            }
            builder.append("\n")
        }

        val sideEffectLogs = todayLogs.filter { it.sideEffectNote.isNotBlank() }
        if (sideEffectLogs.isNotEmpty()) {
            val sec3Title = if (isUk) "3. ПОБІЧНІ ЕФЕКТИ ТА САМОПОЧУТТЯ" else "3. PATIENT REPORTED SIDE EFFECTS & SYMPTOMS"
            builder.append("-----------------------------------------------------\n")
            builder.append(" $sec3Title\n")
            builder.append("-----------------------------------------------------\n")
            sideEffectLogs.forEach { log ->
                builder.append("• ${log.name}: ${log.sideEffectNote}\n")
            }
            builder.append("\n")
        }

        val sec4Title = if (isUk) "4. ПРИМІТКИ ТА РЕКОМЕНДАЦІЇ ЛІКАРЯ" else "4. PHYSICIAN / CLINICIAN OBSERVATIONS & NOTES"
        builder.append("-----------------------------------------------------\n")
        builder.append(" $sec4Title\n")
        builder.append("-----------------------------------------------------\n")
        builder.append("${if (isUk) "Зауваження" else "Notes"}: _______________________________________________\n")
        builder.append("_____________________________________________________\n")
        builder.append("_____________________________________________________\n\n")
        builder.append("${if (isUk) "Підпис лікаря" else "Physician Signature"}: _______________________\n")
        builder.append("${if (isUk) "Дата" else "Date"}: _____________________\n\n")

        builder.append("=====================================================\n")
        builder.append("  Сформовано PillTracker Mobile Health App           \n")
        builder.append("=====================================================\n")

        val reportText = builder.toString()

        try {
            val sanitizedDate = dateStr.replace(":", "-").replace(" ", "_")
            val reportFile = File(context.cacheDir, "PillTracker_Report_$sanitizedDate.txt")
            reportFile.writeText(reportText)

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "PillTracker Report - ${userAccountName.ifBlank { "Patient" }} ($dateStr)")
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val shareIntent = Intent.createChooser(sendIntent, if (isUk) "Поділитися медичним звітом" else "Share Medical Report")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "PillTracker Report - ${userAccountName.ifBlank { "Patient" }} ($dateStr)")
                putExtra(Intent.EXTRA_TEXT, reportText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, if (isUk) "Поділитися медичним звітом" else "Share Medical Report"))
        }
    }
}
