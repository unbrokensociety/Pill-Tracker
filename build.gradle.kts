// Pill Tracker — root build script.
//
// The repository keeps its Kotlin sources flat in the repo root
// ("all files together", next to this script). This sync step copies each
// flat file into the standard Android source tree under app/src/main/java
// before every build, so GitHub Actions always builds the newest code.
//
// The build then uses only the synced app/src tree (the flat files themselves
// are never compiled directly, which keeps Gradle perfectly happy).
//
// v2.4.7 migration note: the app namespace moved from the leftover template
// "com.example" to the real "com.aistudio.meditracker". Old repos may still
// carry a stale app/src/main/java/com/example tree — the sync WIPES the whole
// app/src/main/java directory before copying, so the flat set below is the
// single source of truth and no stale duplicate class can ever collide with
// the new ones (this is what makes the namespace migration self-healing:
// just upload the new flat files, the next build cleans up after the old
// package by itself).

import java.io.File

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
}

val rootDirFile: File = rootProject.projectDir

// The flat file set is the single source of truth for app/src/main/java.
// Wipe the synced tree first: files that were renamed, moved across
// packages (v2.4.7: com.example → com.aistudio.meditracker) or removed
// never linger as stale duplicates.
val syncedJavaTree = File(rootDirFile, "app/src/main/java")
if (syncedJavaTree.isDirectory) {
    syncedJavaTree.deleteRecursively()
    logger.lifecycle("Wiped app/src/main/java — the flat file set is the source of truth.")
}

val flatSources: Map<String, String> = mapOf(
    // ── app root ────────────────────────────────────────────────────
    "MainActivity.kt" to "app/src/main/java/com/aistudio/meditracker/MainActivity.kt",
    // ── ui: screens + view model ────────────────────────────────────
    "HomeScreen.kt" to "app/src/main/java/com/aistudio/meditracker/ui/HomeScreen.kt",
    "AddMedicationScreen.kt" to "app/src/main/java/com/aistudio/meditracker/ui/AddMedicationScreen.kt",
    "CalendarScreen.kt" to "app/src/main/java/com/aistudio/meditracker/ui/CalendarScreen.kt",
    "MedicationsListScreen.kt" to "app/src/main/java/com/aistudio/meditracker/ui/MedicationsListScreen.kt",
    "SettingsScreen.kt" to "app/src/main/java/com/aistudio/meditracker/ui/SettingsScreen.kt",
    "MainViewModel.kt" to "app/src/main/java/com/aistudio/meditracker/ui/MainViewModel.kt",
    // ── ui/components ───────────────────────────────────────────────
    "GlassCard.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/GlassCard.kt",
    "FormTypeIcon.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/FormTypeIcon.kt",
    "PrivacyPolicyDialog.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/PrivacyPolicyDialog.kt",
    "TermsOfServiceDialog.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/TermsOfServiceDialog.kt",
    "LocaleHelper.kt" to "app/src/main/java/com/aistudio/meditracker/ui/locale/LocaleHelper.kt",
    // Detailed first-launch tutorial (+ replay button in Settings).
    "OnboardingTutorial.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/OnboardingTutorial.kt",
    // In-app updater: checks GitHub Releases, downloads the APK via
    // HttpURLConnection and opens the package installer (needs INTERNET +
    // REQUEST_INSTALL_PACKAGES in the manifest + FileProvider path).
    // v2.4.7: split into focused files instead of one 1200+ line monster —
    // UpdateGate (orchestrator UI), UpdateCenter (data/logic),
    // UpdateDialog + UpdateDialogBodies (the card itself).
    "UpdateChecker.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/UpdateChecker.kt",
    "UpdateCenter.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/UpdateCenter.kt",
    "UpdateDialog.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/UpdateDialog.kt",
    "UpdateDialogBodies.kt" to "app/src/main/java/com/aistudio/meditracker/ui/components/UpdateDialogBodies.kt",
    // ── data layer (Room) ───────────────────────────────────────────
    "MedicationDao.kt" to "app/src/main/java/com/aistudio/meditracker/data/MedicationDao.kt",
    "MedicationRepository.kt" to "app/src/main/java/com/aistudio/meditracker/data/MedicationRepository.kt",
    "AppDatabase.kt" to "app/src/main/java/com/aistudio/meditracker/data/AppDatabase.kt",
    "Entities.kt" to "app/src/main/java/com/aistudio/meditracker/data/Entities.kt",
    "SettingsRepository.kt" to "app/src/main/java/com/aistudio/meditracker/data/SettingsRepository.kt",
    // ── reminders ───────────────────────────────────────────────────
    "AlarmScheduler.kt" to "app/src/main/java/com/aistudio/meditracker/alarms/AlarmScheduler.kt",
    "AlarmReceiver.kt" to "app/src/main/java/com/aistudio/meditracker/alarms/AlarmReceiver.kt",
    "BootReceiver.kt" to "app/src/main/java/com/aistudio/meditracker/alarms/BootReceiver.kt",
    // ── theming ─────────────────────────────────────────────────────
    "Theme.kt" to "app/src/main/java/com/aistudio/meditracker/ui/theme/Theme.kt",
    "Color.kt" to "app/src/main/java/com/aistudio/meditracker/ui/theme/Color.kt",
    "Type.kt" to "app/src/main/java/com/aistudio/meditracker/ui/theme/Type.kt",
    "MedicationColors.kt" to "app/src/main/java/com/aistudio/meditracker/ui/theme/MedicationColors.kt",
    // ── manifest / resources ────────────────────────────────────────
    // Manifest with the updater permissions — kept flat so uploads update it too.
    "AndroidManifest.xml" to "app/src/main/AndroidManifest.xml",
    // FileProvider paths (incl. the downloads dir for update APKs).
    "file_paths.xml" to "app/src/main/res/xml/file_paths.xml",
    // Localized string resources. Kept flat in the repo root and synced into
    // the standard res folders before every build (same mechanism as the
    // sources above), so translations update via simple file uploads.
    "strings_en.xml" to "app/src/main/res/values/strings.xml",
    "strings_ru.xml" to "app/src/main/res/values-ru/strings.xml",
    "strings_uk.xml" to "app/src/main/res/values-uk/strings.xml",
    // Fixed launcher icons: the original xxhdpi/xxxhdpi webp files were corrupt
    // (they declared absurd multi-million-pixel dimensions and could fail to
    // decode on some launchers). These are proper 144/192 px replacements.
    "ic_launcher_xxhdpi.webp" to "app/src/main/res/mipmap-xxhdpi/ic_launcher.webp",
    "ic_launcher_xxhdpi_round.webp" to "app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp",
    "ic_launcher_xxxhdpi.webp" to "app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp",
    "ic_launcher_xxxhdpi_round.webp" to "app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp",
    // Flat copy of the app module build script (versionCode/versionName live here).
    // Synced into app/build.gradle.kts before the module is configured.
    "app_build.gradle.kts" to "app/build.gradle.kts",
    // R8 keep-rules for the release build (minifyEnabled = true). Kept flat
    // for the same reason: uploads update it without touching app/.
    "proguard-rules.pro" to "app/proguard-rules.pro"
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
