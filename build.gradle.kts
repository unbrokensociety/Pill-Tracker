// Pill Tracker — root build script.
//
// The repository keeps its Kotlin sources flat in the repo root
// ("all files together", next to this script). This sync step copies each
// flat file into the standard Android source tree under app/src/main/java
// before every build, so GitHub Actions always builds the newest code.
//
// The build then uses only the synced app/src tree (the flat files themselves
// are never compiled directly, which keeps Gradle perfectly happy).

import java.io.File

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
}

val rootDirFile: File = rootProject.projectDir

val flatSources: Map<String, String> = mapOf(
    "MainActivity.kt" to "app/src/main/java/com/example/MainActivity.kt",
    "HomeScreen.kt" to "app/src/main/java/com/example/ui/HomeScreen.kt",
    "AddMedicationScreen.kt" to "app/src/main/java/com/example/ui/AddMedicationScreen.kt",
    "CalendarScreen.kt" to "app/src/main/java/com/example/ui/CalendarScreen.kt",
    "MedicationsListScreen.kt" to "app/src/main/java/com/example/ui/MedicationsListScreen.kt",
    "SettingsScreen.kt" to "app/src/main/java/com/example/ui/SettingsScreen.kt",
    "GlassCard.kt" to "app/src/main/java/com/example/ui/components/GlassCard.kt",
    "MedicationDao.kt" to "app/src/main/java/com/example/data/MedicationDao.kt",
    "MedicationRepository.kt" to "app/src/main/java/com/example/data/MedicationRepository.kt"
)

flatSources.forEach { (flatName, appRelativePath) ->
    val source = File(rootDirFile, flatName)
    if (source.isFile) {
        val target = File(rootDirFile, appRelativePath)
        val needsUpdate = !target.isFile || target.readText() != source.readText()
        if (needsUpdate) {
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
            logger.lifecycle("Synced flat source: {} -> {}", flatName, appRelativePath)
        }
    }
}
