import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.meditracker.zqxpr"
    minSdk = 26
    targetSdk = 36
    // Versioning — the proven scheme from the old builds:
    //   versionCode = 2310 + GITHUB_RUN_NUMBER (CI sets the env var),
    //   so every CI build grows and can NEVER regress. Base 2310 keeps
    //   every build above everything ever shipped (v1.91 = 2401).
    //   versionName is the human version; it MUST match the
    //   «app-version:» marker at the top of RELEASE_NOTES.md — the
    //   in-app updater compares those to detect real updates.
    val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0
    versionCode = 2310 + runNumber
    versionName = "2.3.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    getByName("debug") {
      val ksFile = file("${rootDir}/debug.keystore")
      val ksBase64File = file("${rootDir}/debug.keystore.base64")
      if ((!ksFile.exists() || ksFile.length() == 0L) && ksBase64File.exists()) {
        try {
          val cleanBase64 = ksBase64File.readText().replace("\\s".toRegex(), "")
          val decoded = Base64.getDecoder().decode(cleanBase64)
          ksFile.writeBytes(decoded)
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
      if (ksFile.exists() && ksFile.length() > 0L) {
        storeFile = ksFile
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("release") {
      val releaseKeystorePath = System.getenv("KEYSTORE_PATH")
      val hasCustomReleaseKey = !releaseKeystorePath.isNullOrBlank() && file(releaseKeystorePath).let { it.exists() && it.isFile }
      if (hasCustomReleaseKey) {
        storeFile = file(releaseKeystorePath!!)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS").takeIf { !it.isNullOrBlank() } ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        val ksFile = file("${rootDir}/debug.keystore")
        val ksBase64File = file("${rootDir}/debug.keystore.base64")
        if ((!ksFile.exists() || ksFile.length() == 0L) && ksBase64File.exists()) {
          try {
            val cleanBase64 = ksBase64File.readText().replace("\\s".toRegex(), "")
            val decoded = Base64.getDecoder().decode(cleanBase64)
            ksFile.writeBytes(decoded)
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
        if (ksFile.exists() && ksFile.length() > 0L) {
          storeFile = ksFile
          storePassword = "android"
          keyAlias = "androiddebugkey"
          keyPassword = "android"
        }
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
