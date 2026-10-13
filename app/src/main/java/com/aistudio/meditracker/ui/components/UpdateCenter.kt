package com.aistudio.meditracker.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateCenter {
    private const val API_URL =
        "https://api.github.com/repos/unbrokensociety/Pill-Tracker/releases/latest"
    const val RELEASES_PAGE =
        "https://github.com/unbrokensociety/Pill-Tracker/releases"

    private const val PREFS = "update_center_prefs"
    private const val KEY_SKIPPED_VERSION = "skipped_version_name"
    private const val KEY_LAST_CHECK = "last_check_ms"

    private const val AUTO_CHECK_INTERVAL_MS = 3L * 60 * 60 * 1000

    private const val UPDATES_DIR = "updates"
    private const val APK_FILE_NAME = "pill-tracker-update.apk"
    private const val TMP_FILE_NAME = "pill-tracker-update.tmp"

    data class Release(
        val tag: String,
        val versionName: String?,
        val apkUrl: String,
        val pageUrl: String,
        val noteLines: List<String>
    )

    @Suppress("DEPRECATION")
    fun installedVersionName(context: Context): String? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun shouldAutoCheck(context: Context): Boolean {
        val last = prefs(context).getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last > AUTO_CHECK_INTERVAL_MS
    }

    fun markChecked(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }

    fun isSkipped(context: Context, release: Release): Boolean {
        val v = release.versionName ?: return false
        return prefs(context).getString(KEY_SKIPPED_VERSION, "") == v
    }

    fun skip(context: Context, release: Release) {
        release.versionName?.let {
            prefs(context).edit().putString(KEY_SKIPPED_VERSION, it).apply()
        }
    }

    private fun parseSemver(source: String?): IntArray? {
        if (source.isNullOrBlank()) return null
        return try {
            val m = Regex("\\d+\\.\\d+\\.\\d+").find(source) ?: return null
            val parts = m.value.split('.')
            intArrayOf(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: Exception) {
            null
        }
    }

    fun isNewerThanInstalled(context: Context, release: Release): Boolean = try {
        val remote = parseSemver(release.versionName ?: release.tag)
        val local = parseSemver(installedVersionName(context))
        if (remote != null && local != null) {
            var newer = false
            for (i in 0 until 3) {
                if (remote[i] != local[i]) {
                    newer = remote[i] > local[i]
                    break
                }
            }
            newer
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }

    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Accept", "application/vnd.github+json")

            conn.setRequestProperty("User-Agent", "PillTracker-UpdateChecker")
            try {
                if (conn.responseCode != 200) return@withContext null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseRelease(body)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRelease(body: String): Release? {
        val json = JSONObject(body)
        val tag = json.optString("tag_name", "").trim()
        val rawNotes = json.optString("body", "")
        val pageUrl = json.optString("html_url", "").ifBlank { RELEASES_PAGE }

        val versionName = Regex("app-version:\\s*[vV]?(\\d+\\.\\d+\\.\\d+)", RegexOption.IGNORE_CASE)
            .find(rawNotes)?.groupValues?.getOrNull(1)
            ?: Regex("\\d+\\.\\d+\\.\\d+").find(
                json.optString("name", "").trim()
            )?.value

        var apkUrl = ""
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            val url = a.optString("browser_download_url", "")
            if (url.endsWith(".apk", ignoreCase = true)) {
                apkUrl = url
                break
            }
        }
        if (tag.isBlank() || apkUrl.isBlank()) return null

        val notes = rawNotes
            .substringBefore("\n---")
            .lineSequence()
            .map { it.trim() }
            .filter {
                it.isNotBlank() && !it.startsWith("#") && !it.startsWith(">")
            }
            .map {
                it.removePrefix("- ").removePrefix("* ").removePrefix("• ")
                    .replace("**", "").replace("`", "")
                    .trim()
            }
            .filter { it.length > 2 }
            // Full lines, no mid-sentence "…" cuts — the changelog list in
            // the update dialog scrolls, so long bullets are welcome now.
            .take(10)
            .toList()

        return Release(tag, versionName, apkUrl, pageUrl, notes)
    }

    fun canInstallFromApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            openReleasesPage(context)
        }
    }

    private fun updatesDir(context: Context): File = File(context.cacheDir, UPDATES_DIR)

    suspend fun downloadApk(
        context: Context,
        release: Release,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var tmp: File? = null
        try {
            val dir = updatesDir(context).apply { mkdirs() }
            val target = File(dir, APK_FILE_NAME)
            tmp = File(dir, TMP_FILE_NAME)
            if (tmp.exists()) tmp.delete()

            val conn = URL(release.apkUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "PillTracker-UpdateChecker")
            try {
                if (conn.responseCode !in 200..299) return@withContext null
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        var lastPct = -1
                        while (isActive) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) {
                                val pct = ((done * 100) / total).toInt()
                                if (pct != lastPct) {
                                    lastPct = pct
                                    val f = (done.toFloat() / total).coerceIn(0f, 1f)
                                    withContext(Dispatchers.Main) { onProgress(f) }
                                }
                            }
                        }
                    }
                }
                if (tmp.length() < 10_000) return@withContext null
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) {
                    tmp.copyTo(target, overwrite = true)
                    tmp.delete()
                }
                withContext(Dispatchers.Main) { onProgress(1f) }
                target
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            tmp?.delete()
            null
        }
    }

    fun downloadedApk(context: Context): File? {
        val f = File(updatesDir(context), APK_FILE_NAME)
        return if (f.exists() && f.length() > 10_000) f else null
    }

    fun cleanupDownloads(context: Context) {
        try {
            updatesDir(context).listFiles()?.forEach { it.delete() }

            @Suppress("DEPRECATION")
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { ext ->
                File(ext, APK_FILE_NAME).takeIf { it.exists() }?.delete()
            }

            context.cacheDir.listFiles()?.forEach { f ->
                val n = f.name.lowercase()
                if (f.isFile && (n.endsWith(".apk") || n.endsWith(".tmp"))) {
                    f.delete()
                }
            }

            @Suppress("DEPRECATION")
            context.getExternalFilesDir(null)?.let { ext ->
                File(ext, "Download").takeIf { it.isDirectory }?.listFiles()
                    ?.forEach { it.delete() }
            }
        } catch (_: Exception) {
        }
    }

    fun installApk(context: Context, apk: File): Boolean {
        if (!apk.exists() || apk.length() < 10_000) return false
        return try {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", apk
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openReleasesPage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
