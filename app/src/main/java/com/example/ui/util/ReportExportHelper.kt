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
        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val builder = StringBuilder()

        val totalScheduled = schedules.size
        val takenCount = todayLogs.size
        val adherenceRate = if (totalScheduled > 0) ((takenCount.toDouble() / totalScheduled.toDouble()) * 100).toInt() else 100

        builder.append("=====================================================\n")
        builder.append("         CLINICAL & PHARMACOLOGICAL REPORT           \n")
        builder.append("                    PILL TRACKER                     \n")
        builder.append("=====================================================\n")
        builder.append("Report Timestamp : $dateStr\n")
        builder.append("Patient Name     : ${userAccountName.ifBlank { "Guest Patient" }}\n")
        if (userEmail.isNotBlank()) {
            builder.append("Account Email    : $userEmail\n")
        }
        builder.append("Adherence Rate   : $adherenceRate% today ($takenCount/$totalScheduled doses completed)\n")
        builder.append("Compliance Streak: $streakDays consecutive days active\n\n")

        builder.append("-----------------------------------------------------\n")
        builder.append(" 1. PRESCRIPTION & STOCK REGISTRY (${medications.size} Active)\n")
        builder.append("-----------------------------------------------------\n")
        if (medications.isEmpty()) {
            builder.append("No active medications registered in system.\n\n")
        } else {
            medications.forEachIndexed { index, med ->
                val stockAlert = if (med.stockCount <= med.lowStockThreshold) "⚠️ LOW STOCK REFILL NEEDED!" else "OK"
                builder.append("[${index + 1}] ${med.name.uppercase()} — ${med.dosage}\n")
                if (med.notes.isNotBlank()) builder.append("    Clinical Notes  : ${med.notes}\n")
                builder.append("    Daily Frequency : ${med.timesPerDay} intake(s)/day\n")
                builder.append("    Current Inventory: ${med.stockCount} pills (Threshold: ${med.lowStockThreshold}) [$stockAlert]\n\n")
            }
        }

        builder.append("-----------------------------------------------------\n")
        builder.append(" 2. TODAY'S SCHEDULE & COMPLIANCE LOG\n")
        builder.append("-----------------------------------------------------\n")
        if (schedules.isEmpty()) {
            builder.append("No medication doses scheduled for today.\n\n")
        } else {
            schedules.forEach { sch ->
                val matchingLog = todayLogs.find { it.scheduleId == sch.scheduleId }
                val isTaken = matchingLog != null
                val timeFormatted = String.format("%02d:%02d", sch.timeHour, sch.timeMinute)
                val statusSymbol = if (isTaken) "✓ TAKEN" else "✗ PENDING"
                
                builder.append("• $timeFormatted - ${sch.name} (${sch.dosage}) -> $statusSymbol")
                if (matchingLog != null && matchingLog.sideEffectNote.isNotBlank()) {
                    builder.append(" [Side Effect Logged: ${matchingLog.sideEffectNote}]")
                }
                builder.append("\n")
            }
            builder.append("\n")
        }

        val sideEffectLogs = todayLogs.filter { it.sideEffectNote.isNotBlank() }
        if (sideEffectLogs.isNotEmpty()) {
            builder.append("-----------------------------------------------------\n")
            builder.append(" 3. PATIENT REPORTED SIDE EFFECTS & SYMPTOMS\n")
            builder.append("-----------------------------------------------------\n")
            sideEffectLogs.forEach { log ->
                builder.append("• ${log.name}: ${log.sideEffectNote}\n")
            }
            builder.append("\n")
        }

        builder.append("-----------------------------------------------------\n")
        builder.append(" 4. PHYSICIAN / CLINICIAN OBSERVATIONS & NOTES\n")
        builder.append("-----------------------------------------------------\n")
        builder.append("Notes: _______________________________________________\n")
        builder.append("_____________________________________________________\n")
        builder.append("_____________________________________________________\n\n")
        builder.append("Attending Physician Signature: _______________________\n")
        builder.append("Date: _____________________\n\n")

        builder.append("=====================================================\n")
        builder.append("  Generated securely by PillTracker Mobile Health    \n")
        builder.append("=====================================================\n")

        val reportText = builder.toString()

        try {
            val reportFile = File(context.cacheDir, "PillTracker_Report_$dateStr.txt")
            reportFile.writeText(reportText)

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "PillTracker Medical Report - ${userAccountName.ifBlank { "Patient" }} ($dateStr)")
                putExtra(Intent.EXTRA_TEXT, reportText)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val shareIntent = Intent.createChooser(sendIntent, "Share Medical Report")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Direct text fallback
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "PillTracker Medical Report - ${userAccountName.ifBlank { "Patient" }} ($dateStr)")
                putExtra(Intent.EXTRA_TEXT, reportText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Medical Report"))
        }
    }
}

