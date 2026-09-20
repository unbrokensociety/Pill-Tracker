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
    "FormTypeIcon.kt" to "app/src/main/java/com/example/ui/components/FormTypeIcon.kt",
    "MedicationDao.kt" to "app/src/main/java/com/example/data/MedicationDao.kt",
    "MedicationRepository.kt" to "app/src/main/java/com/example/data/MedicationRepository.kt",
    "AppDatabase.kt" to "app/src/main/java/com/example/data/AppDatabase.kt",
    "PrivacyPolicyDialog.kt" to "app/src/main/java/com/example/ui/components/PrivacyPolicyDialog.kt",
    "TermsOfServiceDialog.kt" to "app/src/main/java/com/example/ui/components/TermsOfServiceDialog.kt",
    "LocaleHelper.kt" to "app/src/main/java/com/example/ui/locale/LocaleHelper.kt",
    // Fixed launcher icons: the original xxhdpi/xxxhdpi webp files were corrupt
    // (they declared absurd multi-million-pixel dimensions and could fail to
    // decode on some launchers). These are proper 144/192 px replacements.
    "ic_launcher_xxhdpi.webp" to "app/src/main/res/mipmap-xxhdpi/ic_launcher.webp",
    "ic_launcher_xxhdpi_round.webp" to "app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp",
    "ic_launcher_xxxhdpi.webp" to "app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp",
    "ic_launcher_xxxhdpi_round.webp" to "app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp",
    // Flat copy of the app module build script (versionCode/versionName live here).
    // Synced into app/build.gradle.kts before the module is configured.
    "app_build.gradle.kts" to "app/build.gradle.kts"
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
