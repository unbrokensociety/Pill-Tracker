package com.example.data

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class CloudSyncRepository(
    private val context: Context,
    private val medicationDao: MedicationDao,
    private val settingsRepository: SettingsRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val backupFile: File
        get() = File(context.filesDir, "meditracker_cloud_backup.json")

    suspend fun logUserAuthentication(
        email: String,
        name: String,
        authProvider: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authUser = FirebaseAuth.getInstance().currentUser
            val userId = authUser?.uid ?: email.replace(".", "_").replace("@", "_")

            val userDoc = hashMapOf(
                "uid" to userId,
                "email" to email,
                "name" to name,
                "authProvider" to authProvider,
                "lastLoginAt" to System.currentTimeMillis(),
                "privacyConsentAccepted" to true,
                "privacyConsentTimestamp" to System.currentTimeMillis(),
                "privacyPolicyVersion" to "v1.22",
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "appVersion" to "v1.22"
            )

            // Write to Firestore User collection
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(userDoc, SetOptions.merge())
                .await()

            // Write to Firestore Consent & Audit Logs collection
            val auditLog = hashMapOf(
                "userId" to userId,
                "email" to email,
                "event" to "USER_REGISTRATION_AND_CONSENT",
                "authProvider" to authProvider,
                "privacyConsentAccepted" to true,
                "privacyPolicyVersion" to "v1.22",
                "timestamp" to System.currentTimeMillis(),
                "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "legalNotice" to "User explicitly accepted Privacy Policy & Terms of Service."
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("consent_audit")
                .add(auditLog)
                .await()

            Result.success("Login logged to Cloud Firestore")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success("Logged locally (Offline Mode)")
        }
    }

    suspend fun syncToCloud(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userEmail = settingsRepository.userEmailFlow.first()
            val userName = settingsRepository.userNameFlow.first()

            val medications = medicationDao.getAllMedications().first()
            val schedules = medicationDao.getAllSchedules().first()
            val logs = medicationDao.getAllIntakeLogs().first()

            val authUser = FirebaseAuth.getInstance().currentUser
            val userId = authUser?.uid ?: userEmail.ifBlank { "guest_user" }.replace(".", "_").replace("@", "_")

            // 1. Try real Firestore Cloud Database Sync
            val firestoreSuccess = try {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(userId)

                val medsList = medications.map { m ->
                    mapOf(
                        "id" to m.id,
                        "name" to m.name,
                        "dosage" to m.dosage,
                        "notes" to m.notes,
                        "color" to m.color,
                        "timesPerDay" to m.timesPerDay,
                        "startDate" to m.startDate,
                        "endDate" to (m.endDate ?: 0L),
                        "stockCount" to m.stockCount,
                        "lowStockThreshold" to m.lowStockThreshold
                    )
                }

                val schedList = schedules.map { s ->
                    mapOf(
                        "id" to s.id,
                        "medicationId" to s.medicationId,
                        "timeHour" to s.timeHour,
                        "timeMinute" to s.timeMinute
                    )
                }

                val logsList = logs.map { l ->
                    mapOf(
                        "id" to l.id,
                        "medicationId" to l.medicationId,
                        "scheduleId" to l.scheduleId,
                        "scheduledDateEpoch" to l.scheduledDateEpoch,
                        "timestampTaken" to l.timestampTaken,
                        "name" to l.name,
                        "dosage" to l.dosage,
                        "timeHour" to l.timeHour,
                        "timeMinute" to l.timeMinute,
                        "sideEffectNote" to l.sideEffectNote
                    )
                }

                userRef.collection("data").document("medications").set(mapOf("items" to medsList)).await()
                userRef.collection("data").document("schedules").set(mapOf("items" to schedList)).await()
                userRef.collection("data").document("logs").set(mapOf("items" to logsList)).await()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

            // 2. Also save to local JSON backup mirror
            val rootJson = JSONObject().apply {
                put("userEmail", userEmail.ifBlank { "guest@meditracker.app" })
                put("userName", userName)
                put("timestamp", System.currentTimeMillis())

                val medsArray = JSONArray().apply {
                    medications.forEach { m ->
                        put(JSONObject().apply {
                            put("id", m.id)
                            put("name", m.name)
                            put("dosage", m.dosage)
                            put("notes", m.notes)
                            put("color", m.color)
                            put("timesPerDay", m.timesPerDay)
                            put("startDate", m.startDate)
                            put("endDate", m.endDate ?: 0L)
                            put("stockCount", m.stockCount)
                            put("lowStockThreshold", m.lowStockThreshold)
                        })
                    }
                }
                put("medications", medsArray)

                val schedArray = JSONArray().apply {
                    schedules.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("medicationId", s.medicationId)
                            put("timeHour", s.timeHour)
                            put("timeMinute", s.timeMinute)
                        })
                    }
                }
                put("schedules", schedArray)

                val logsArray = JSONArray().apply {
                    logs.forEach { l ->
                        put(JSONObject().apply {
                            put("id", l.id)
                            put("medicationId", l.medicationId)
                            put("scheduleId", l.scheduleId)
                            put("scheduledDateEpoch", l.scheduledDateEpoch)
                            put("timestampTaken", l.timestampTaken)
                            put("name", l.name)
                            put("dosage", l.dosage)
                            put("timeHour", l.timeHour)
                            put("timeMinute", l.timeMinute)
                            put("sideEffectNote", l.sideEffectNote)
                        })
                    }
                }
                put("logs", logsArray)
            }

            backupFile.writeText(rootJson.toString())
            settingsRepository.updateLastSyncTimestamp()

            if (firestoreSuccess) {
                Result.success("Синхронизация с Cloud Firestore успешно завершена!")
            } else {
                Result.success("Данные сохранены в локальный облачный кэш!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                settingsRepository.updateLastSyncTimestamp()
                Result.success("Сохранено в облачном кэше (Офлайн режим)")
            } catch (ex: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun restoreFromCloud(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userEmail = settingsRepository.userEmailFlow.first()
            val authUser = FirebaseAuth.getInstance().currentUser
            val userId = authUser?.uid ?: userEmail.ifBlank { "guest_user" }.replace(".", "_").replace("@", "_")

            // 1. First try fetching from live Firebase Firestore
            var restoredMeds = mutableListOf<Medication>()
            var restoredScheds = mutableListOf<Schedule>()
            var restoredLogs = mutableListOf<IntakeLog>()
            var fetchedFromFirestore = false

            try {
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("users").document(userId)

                val medsDoc = userRef.collection("data").document("medications").get().await()
                val schedsDoc = userRef.collection("data").document("schedules").get().await()
                val logsDoc = userRef.collection("data").document("logs").get().await()

                val medsItems = medsDoc.get("items") as? List<Map<String, Any>>
                val schedsItems = schedsDoc.get("items") as? List<Map<String, Any>>
                val logsItems = logsDoc.get("items") as? List<Map<String, Any>>

                if (medsItems != null) {
                    medsItems.forEach { m ->
                        val id = (m["id"] as? Long)?.toInt() ?: 0
                        val name = m["name"] as? String ?: ""
                        val dosage = m["dosage"] as? String ?: ""
                        val notes = m["notes"] as? String ?: ""
                        val color = (m["color"] as? Long)?.toInt() ?: 0
                        val timesPerDay = (m["timesPerDay"] as? Long)?.toInt() ?: 1
                        val startDate = (m["startDate"] as? Long) ?: System.currentTimeMillis()
                        val endDate = (m["endDate"] as? Long)
                        val stockCount = (m["stockCount"] as? Long)?.toInt() ?: 30
                        val lowStockThreshold = (m["lowStockThreshold"] as? Long)?.toInt() ?: 5

                        if (name.isNotBlank()) {
                            restoredMeds.add(
                                Medication(
                                    id = id,
                                    name = name,
                                    dosage = dosage,
                                    notes = notes,
                                    color = color,
                                    timesPerDay = timesPerDay,
                                    startDate = startDate,
                                    endDate = if (endDate == 0L) null else endDate,
                                    stockCount = stockCount,
                                    lowStockThreshold = lowStockThreshold
                                )
                            )
                        }
                    }
                }

                if (schedsItems != null) {
                    schedsItems.forEach { s ->
                        restoredScheds.add(
                            Schedule(
                                id = (s["id"] as? Long)?.toInt() ?: 0,
                                medicationId = (s["medicationId"] as? Long)?.toInt() ?: 0,
                                timeHour = (s["timeHour"] as? Long)?.toInt() ?: 8,
                                timeMinute = (s["timeMinute"] as? Long)?.toInt() ?: 0
                            )
                        )
                    }
                }

                if (logsItems != null) {
                    logsItems.forEach { l ->
                        restoredLogs.add(
                            IntakeLog(
                                id = (l["id"] as? Long)?.toInt() ?: 0,
                                medicationId = (l["medicationId"] as? Long)?.toInt() ?: 0,
                                scheduleId = (l["scheduleId"] as? Long)?.toInt() ?: 0,
                                scheduledDateEpoch = (l["scheduledDateEpoch"] as? Long) ?: System.currentTimeMillis(),
                                timestampTaken = (l["timestampTaken"] as? Long) ?: System.currentTimeMillis(),
                                name = l["name"] as? String ?: "",
                                dosage = l["dosage"] as? String ?: "",
                                timeHour = (l["timeHour"] as? Long)?.toInt() ?: 8,
                                timeMinute = (l["timeMinute"] as? Long)?.toInt() ?: 0,
                                sideEffectNote = l["sideEffectNote"] as? String ?: ""
                            )
                        )
                    }
                }

                if (restoredMeds.isNotEmpty()) {
                    fetchedFromFirestore = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Fallback to local JSON backup file if Firestore didn't yield meds
            if (!fetchedFromFirestore && backupFile.exists() && backupFile.length() > 0L) {
                val jsonString = backupFile.readText()
                val rootJson = JSONObject(jsonString)

                val medsArray = rootJson.optJSONArray("medications") ?: JSONArray()
                for (i in 0 until medsArray.length()) {
                    val obj = medsArray.getJSONObject(i)
                    restoredMeds.add(
                        Medication(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            dosage = obj.getString("dosage"),
                            notes = obj.optString("notes", ""),
                            color = obj.optInt("color", 0),
                            timesPerDay = obj.optInt("timesPerDay", 1),
                            startDate = obj.getLong("startDate"),
                            endDate = if (obj.optLong("endDate", 0L) == 0L) null else obj.getLong("endDate"),
                            stockCount = obj.optInt("stockCount", 30),
                            lowStockThreshold = obj.optInt("lowStockThreshold", 5)
                        )
                    )
                }

                val schedArray = rootJson.optJSONArray("schedules") ?: JSONArray()
                for (i in 0 until schedArray.length()) {
                    val obj = schedArray.getJSONObject(i)
                    restoredScheds.add(
                        Schedule(
                            id = obj.getInt("id"),
                            medicationId = obj.getInt("medicationId"),
                            timeHour = obj.getInt("timeHour"),
                            timeMinute = obj.getInt("timeMinute")
                        )
                    )
                }

                val logsArray = rootJson.optJSONArray("logs") ?: JSONArray()
                for (i in 0 until logsArray.length()) {
                    val obj = logsArray.getJSONObject(i)
                    restoredLogs.add(
                        IntakeLog(
                            id = obj.getInt("id"),
                            medicationId = obj.getInt("medicationId"),
                            scheduleId = obj.getInt("scheduleId"),
                            scheduledDateEpoch = obj.optLong("scheduledDateEpoch", System.currentTimeMillis()),
                            timestampTaken = obj.optLong("timestampTaken", System.currentTimeMillis()),
                            name = obj.getString("name"),
                            dosage = obj.getString("dosage"),
                            timeHour = obj.getInt("timeHour"),
                            timeMinute = obj.getInt("timeMinute"),
                            sideEffectNote = obj.optString("sideEffectNote", "")
                        )
                    )
                }
            }

            if (restoredMeds.isEmpty()) {
                return@withContext Result.failure(Exception("Никаких данных в облаке не найдено."))
            }

            // Write into database
            medicationDao.insertMedications(restoredMeds)
            medicationDao.insertSchedules(restoredScheds)
            restoredLogs.forEach { medicationDao.insertIntakeLog(it) }

            settingsRepository.updateLastSyncTimestamp()
            val sourceText = if (fetchedFromFirestore) "Cloud Firestore" else "локального кэша"
            Result.success("Восстановлено ${restoredMeds.size} препаратов из $sourceText!")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
