import java.io.File
import java.util.Base64

/**
 * Decodes debug.keystore from its flat base64 twin (if present) and returns
 * the keystore file when it is actually usable. Shared by the debug and the
 * explicit debug-fallback release signing configs.
 */
fun ensureDebugKeystore(rootDir: File): File? {
    val ksFile = File(rootDir, "debug.keystore")
    val ksBase64File = File(rootDir, "debug.keystore.base64")
    if ((!ksFile.exists() || ksFile.length() == 0L) && ksBase64File.exists()) {
        try {
            val cleanBase64 = ksBase64File.readText().replace("\\s".toRegex(), "")
            ksFile.writeBytes(Base64.getDecoder().decode(cleanBase64))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return ksFile.takeIf { it.exists() && it.length() > 0L }
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.aistudio.meditracker"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.meditracker.zqxpr"
    minSdk = 26
    targetSdk = 36
    // versionCode = 2420 + GITHUB_RUN_NUMBER so CI builds never regress;
    // versionName must match the app-version marker in RELEASE_NOTES.md.
    val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
    versionCode = 2420 + runNumber
    versionName = "2.5.6"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    getByName("debug") {
      val ksFile = ensureDebugKeystore(rootDir)
      if (ksFile != null) {
        storeFile = ksFile
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("release") {
      // 1) real keystore via KEYSTORE_PATH env → production signing;
      // 2) explicit opt-in (gradle.properties / -P flag / env var) →
      //    debug key + loud warning banner (the CI/distribution path);
      // 3) otherwise release packaging fails with instructions.
      val releaseKeystorePath = System.getenv("KEYSTORE_PATH")
      val hasCustomReleaseKey = !releaseKeystorePath.isNullOrBlank() && file(releaseKeystorePath).let { it.exists() && it.isFile }
      if (hasCustomReleaseKey) {
        storeFile = file(releaseKeystorePath!!)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS").takeIf { !it.isNullOrBlank() } ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        val optInDebugSigning = (
            providers.gradleProperty("allowDebugSigning").getOrElse("false")
              .equals("true", ignoreCase = true)
            ) || (
            System.getenv("ALLOW_DEBUG_SIGNING")?.equals("true", ignoreCase = true) ?: false
            )
        if (optInDebugSigning) {
          val ksFile = ensureDebugKeystore(rootDir)
          if (ksFile != null) {
            storeFile = ksFile
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            // Visible from both the Gradle console and the CI logs:
            logger.warn(
              "╔══════════════════════════════════════════════════════════════╗\n" +
              "║  ⚠  RELEASE APK SIGNED WITH THE DEBUG KEY (explicit opt-in)  ║\n" +
              "║  This is a CI/distribution convenience, NOT a production      ║\n" +
              "║  signature. Do NOT ship this artifact to a store.             ║\n" +
              "╚══════════════════════════════════════════════════════════════╝"
            )
            // Machine-readable flag for CI steps (artifact naming/notices).
            project.extra["releaseSignedWithDebugKey"] = true
          }
          // ksFile == null → AGP default debug keystore; already warned above.
        } else {
          // Fail only when release packaging is actually requested.
          val requested = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
          val wantsReleasePackaging = requested.any { n ->
            n.equals("assemble", true) || n.equals("build", true) || n.equals(
              "assembleRelease", true
            ) || (n.endsWith("Release", true) && (
                n.startsWith("assemble") || n.startsWith("package") ||
                    n.startsWith("bundle") || n.startsWith("install")))
          }
          if (wantsReleasePackaging) {
            throw GradleException(
              """RELEASE SIGNING IS NOT CONFIGURED — and silent fallbacks are OFF.

  This build would have produced a "release" APK secretly signed with the
  debug key. That is fine for CI convenience builds, but it must never
  happen by accident in a real production distribution.

  Pick ONE:
    a) Provide a real release keystore:
         KEYSTORE_PATH=/path/release.keystore \
         STORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... gradle assembleRelease
       (in CI these come from the KEYSTORE_BASE64 / STORE_PASSWORD /
        KEY_ALIAS / KEY_PASSWORD secrets — see android.yml)
    b) Explicitly accept a debug-signed build — ONE line, no flags, no
       workflow edits (this is what the shipped gradle.properties does):
         echo 'allowDebugSigning=true' >> gradle.properties
       (or: gradle assembleRelease -PallowDebugSigning=true)
       The artifact will be loudly marked as debug-signed in the logs.""".trimIndent()
            )
          }
        }
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8 shrinking keeps the icon/material libraries from bloating the
      // dex — minified it is roughly 4x smaller, so the download and the
      // installed footprint stay small.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Signing: either the real release key (KEYSTORE_PATH) or the
      // explicitly opted-in debug key — see the signingConfigs policy
      // above. The debug fallback here only covers the pathological
      // "opted in but no debug.keystore file at all" case (AGP's default).
      val relConfig = signingConfigs.getByName("release")
      signingConfig = if (relConfig.storeFile != null && relConfig.storeFile?.exists() == true) {
        relConfig
      } else {
        signingConfigs.getByName("debug")
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.zxing.core)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
