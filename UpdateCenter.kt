package com.aistudio.meditracker.ui.components

/*
 * UpdateCenter — данные и логика самообновления (v2.4.7: вынесено из
 * UpdateChecker.kt, код без изменений). Чистый Kotlin + HttpURLConnection:
 * версии, троттлинг, «позже», парсинг GitHub-релиза, скрытое скачивание
 * в cacheDir, чистка следов и запуск установщика.
 */

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

    /** Проверяем автоматически не чаще, чем раз в 3 часа. */
    private const val AUTO_CHECK_INTERVAL_MS = 3L * 60 * 60 * 1000

    /* APK живёт в cacheDir — невидим снаружи, чистится сам. */
    private const val UPDATES_DIR = "updates"
    private const val APK_FILE_NAME = "pill-tracker-update.apk"
    private const val TMP_FILE_NAME = "pill-tracker-update.tmp"

    data class Release(
        val tag: String,             // «v1.92»
        val versionName: String?,    // «2.3.2» — из маркера «app-version:» в теле релиза
        val apkUrl: String,          // прямой URL .apk из assets
        val pageUrl: String,         // страница релиза
        val noteLines: List<String>  // короткий список «что нового»
    )

    /* ── версия установленного приложения ── */

    @Suppress("DEPRECATION")
    fun installedVersionName(context: Context): String? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }

    /* ── SharedPreferences: троттлинг и «позже» ── */

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun shouldAutoCheck(context: Context): Boolean {
        val last = prefs(context).getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last > AUTO_CHECK_INTERVAL_MS
    }

    fun markChecked(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }

    /** Эту версию уже откладывали «Позже»? (запоминаем по ИМЕНИ версии) */
    fun isSkipped(context: Context, release: Release): Boolean {
        val v = release.versionName ?: return false
        return prefs(context).getString(KEY_SKIPPED_VERSION, "") == v
    }

    fun skip(context: Context, release: Release) {
        release.versionName?.let {
            prefs(context).edit().putString(KEY_SKIPPED_VERSION, it).apply()
        }
    }

    /* ── сравнение версий: semver по ИМЕНИ ── */

    /** «2.3.1», «2.3.1 (#89)», «Pill Tracker 2.3.1» → [2, 3, 1]. */
    private fun parseSemver(source: String?): IntArray? {
        if (source.isNullOrBlank()) return null
        return try {
            // ВАЖНО: у регулярки нет групп — берём ВСЁ совпадение и режем
            // по точкам. (groupValues[1..3] на regex без групп кидают
            // IndexOutOfBoundsException — это и был краш при проверке
            // обновлений в v2.3.1.)
            val m = Regex("\\d+\\.\\d+\\.\\d+").find(source) ?: return null
            val parts = m.value.split('.')
            intArrayOf(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Релиз свежее установленного?
     *
     * Источник правды — ИМЯ версии из маркера «app-version: X.Y.Z»,
     * который мы сами пишем в самое начало RELEASE_NOTES.md (он же
     * попадает в тело GitHub-релиза). Пересборка ТОЙ ЖЕ версии CI-ем
     * (вырос номер ранка, имя не поменялось) — НЕ обновление: авто-чек
     * молчит, ручная проверка говорит «у вас последняя версия».
     * Если маркера нет (старый релиз) или имя не спарсилось — молчим:
     * лучше один раз поставить руками, чем дёргать ложной карточкой.
     */
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
            false // ничего достоверно не спарсили → не пугаем карточкой
        }
    } catch (e: Exception) {
        // Ни одно исключение здесь не должно ронять приложение —
        // проверка обновлений вторична по отношению к работе трекера.
        false
    }

    /* ── GitHub API ── */

    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            // GitHub требует осмысленный User-Agent
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

        // Имя версии: ПЕРВЫМ ДЕЛОМ маркер «app-version: X.Y.Z», который мы
        // пишем в начало RELEASE_NOTES.md (CI кладёт файл в тело релиза
        // как есть). Название релиза CI даёт как «Pill Tracker v1.92» —
        // семвера там нет, поэтому маркер — единственный надёжный источник.
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

        // Заметки: КОМПАКТНО. Берём строки до разделителя «---» (тех.
        // подпись CI не показываем), выкидываем заголовки «#…»/«###…» и
        // служебный маркер «> app-version:…», снимаем markdown, длинные
        // строки режем до ~112 символов — в диалоге максимум 3 короткие
        // строки, а не стена текста.
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
            .map { line ->
                if (line.length > 112) line.take(109).trimEnd() + "…" else line
            }
            .take(3)
            .toList()

        return Release(tag, versionName, apkUrl, pageUrl, notes)
    }

    /* ── разрешение на установку (один раз, Android 8+) ── */

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

    /* ── скачивание: скрытое, в cacheDir ── */

    private fun updatesDir(context: Context): File = File(context.cacheDir, UPDATES_DIR)

    /**
     * Качает APK обычным HTTP в cacheDir/updates/ — без DownloadManager,
     * без уведомления, файл не виден в проводнике и системных «Загрузках».
     * onProgress: 0..1 или -1, если сервер не отдал размер.
     */
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

    /** Уже скачанный и целый APK (если есть). */
    fun downloadedApk(context: Context): File? {
        val f = File(updatesDir(context), APK_FILE_NAME)
        return if (f.exists() && f.length() > 10_000) f else null
    }

    /**
     * Удаляем следы обновлений — и «мусор прошлых эпох». Вызывается на
     * каждом старте: первый запуск НОВОЙ версии подчищает APK, который
     * скачала старая, плюс наследие v2.3.0 (DownloadManager в external
     * files) и любые потерянные .apk/.tmp в корне кеша — после обновления
     * на диске не остаётся ничего лишнего.
     */
    fun cleanupDownloads(context: Context) {
        try {
            updatesDir(context).listFiles()?.forEach { it.delete() }
            // наследие v2.3.0: DownloadManager писал в external files
            @Suppress("DEPRECATION")
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { ext ->
                File(ext, APK_FILE_NAME).takeIf { it.exists() }?.delete()
            }
            // страховка: потерянные apk/tmp прямо в корне cacheDir
            context.cacheDir.listFiles()?.forEach { f ->
                val n = f.name.lowercase()
                if (f.isFile && (n.endsWith(".apk") || n.endsWith(".tmp"))) {
                    f.delete()
                }
            }
            // и старый каталог «Download» в external files, если заводился
            @Suppress("DEPRECATION")
            context.getExternalFilesDir(null)?.let { ext ->
                File(ext, "Download").takeIf { it.isDirectory }?.listFiles()
                    ?.forEach { it.delete() }
            }
        } catch (_: Exception) {
        }
    }

    /* ── установка ── */

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
