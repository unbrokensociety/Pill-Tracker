package com.example.ui.components

/*
 * UpdateChecker v3 — самообновление целиком внутри приложения.
 *
 * Что изменилось против v2.3.1 (краш и ложные «доступно обновление»):
 *  • КРАШ ПРИ ПРОВЕРКЕ ОБНОВЛЕНИЙ устранён: parseSemver читал
 *    groupValues[1..3] у регулярки БЕЗ групп — IndexOutOfBoundsException
 *    не ловился catch(NumberFormatException) и ронял всё приложение на
 *    каждой проверке (авто при старте и по кнопке). Теперь совпадение
 *    режется по точкам, а любые исключения глотаются.
 *  • Версия релиза определяется по МАРКЕРУ «app-version: X.Y.Z» в начале
 *    RELEASE_NOTES.md (CI кладёт файл в тело релиза как есть). Это
 *    надёжнее названия релиза («Pill Tracker v1.92» — семвера нет) и
 *    безопаснее старого парсинга «versionCode …» из тела (в старых
 *    заметках оставался текст «versionCode 2100» → вечное «доступно
 *    обновление»). Пересборка ТОЙ ЖЕ версии — НЕ обновление.
 *  • Заметки в диалоге — КОМПАКТНЫ: максимум 3 строки по ~112 символов,
 *    без markdown-заголовков («# …»), тех. подписи CI отрезаны по «---».
 *  • APK качает HttpURLConnection прямо в cacheDir/updates/ — файл
 *    невидим: его нет в проводнике, нет уведомления. Установщик
 *    открывается сам по завершении (или по ON_RESUME из фона);
 *    после установки новая версия подчищает файл за собой.
 *
 * Поток:
 *  1. Автопроверка при старте (не чаще 3 ч) / кнопка в Настройках →
 *     GitHub API /releases/latest.
 *  2. Есть новая версия → карточка «Доступно обновление» с заметками.
 *  3. «Обновить»: если нет разрешения на установку из приложения —
 *     инструкция (3 шага) + ACTION_MANAGE_UNKNOWN_APP_SOURCES; вернулся —
 *     всё продолжается само.
 *  4. Скачивание в cacheDir с процентами → установщик открывается сам,
 *     остаётся нажать «Установить» (этого требует Android — молча ставить
 *     поверх себя нельзя никому, кроме Play Store).
 *  5. Fallback: «Скачать через браузер» / страница релизов.
 *
 * Никаких сторонних библиотек: HttpURLConnection + org.json + FileProvider.
 */

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/* ────────────────────────────────────────────────────────────────
 * Шина «проверь обновления вручную» (кнопка в Настройках)
 * ──────────────────────────────────────────────────────────────── */

object UpdateBus {
    var checkRequested by mutableStateOf(false)
        private set

    fun requestCheck() {
        checkRequested = true
    }

    fun consumeCheck() {
        checkRequested = false
    }
}

/* ────────────────────────────────────────────────────────────────
 * Логика проверки / скачивания / установки
 * ──────────────────────────────────────────────────────────────── */

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

/* ────────────────────────────────────────────────────────────────
 * UI: gate-компонент + карточка диалога
 * ──────────────────────────────────────────────────────────────── */

private enum class UpdateUi {
    CHECKING,
    ASKING,               // «Доступно обновление» + кнопки
    NEED_PERMISSION,      // инструкция «разреши один раз»
    AWAITING_PERMISSION,  // ушли в настройки, ждём возвращения
    DOWNLOADING,
    UP_TO_DATE,
    FAILED
}

/**
 * Ставится в MainScreen поверх всего контента. Сам решает, когда
 * показаться (автопроверка при старте / ручная проверка из Настроек),
 * и рисует карточку обновления.
 */
@Composable
fun UpdateGate() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<UpdateUi?>(null) }
    var release by remember { mutableStateOf<UpdateCenter.Release?>(null) }
    var progress by remember { mutableStateOf(-1f) } // -1 = размер неизвестен
    var apkFile by remember { mutableStateOf<File?>(null) }
    var pendingInstall by remember { mutableStateOf(false) }

    /* ── Первым делом подчищаем следы прошлых обновлений ── */
    LaunchedEffect(Unit) {
        UpdateCenter.cleanupDownloads(context)
    }

    /* ── Открыть установщик (с повтором, если приложение было в фоне) ── */
    fun tryInstall() {
        val f = apkFile ?: return
        if (UpdateCenter.installApk(context, f)) {
            pendingInstall = false
            uiState = null // установщик открыт
        } else {
            // Приложение в фоне (Android не даёт стартовать активити из фона) —
            // попробуем снова, когда пользователь вернётся (ON_RESUME).
            pendingInstall = true
        }
    }

    /* ── Проверка (общая для авто и ручной) ── */
    fun runCheck(manual: Boolean) {
        if (manual) uiState = UpdateUi.CHECKING
        scope.launch {
            try {
                val r = UpdateCenter.fetchLatest()
                UpdateCenter.markChecked(context)
                if (r == null) {
                    uiState = UpdateUi.FAILED
                } else if (UpdateCenter.isNewerThanInstalled(context, r) &&
                    !UpdateCenter.isSkipped(context, r)
                ) {
                    release = r
                    uiState = UpdateUi.ASKING
                } else if (manual) {
                    uiState = UpdateUi.UP_TO_DATE
                } else {
                    uiState = null // авто-проверка молчит, когда всё актуально
                }
            } catch (e: Exception) {
                // Страховка: проверка обновлений никогда не роняет приложение.
                uiState = if (manual) UpdateUi.FAILED else null
            }
        }
    }

    /* ── Ручная проверка из Настроек ── */
    LaunchedEffect(Unit) {
        snapshotFlow { UpdateBus.checkRequested }
            .filter { it }
            .collect {
                UpdateBus.consumeCheck()
                runCheck(manual = true)
            }
    }

    /* ── Автопроверка при запуске (не чаще раза в 3 часа) ── */
    LaunchedEffect(Unit) {
        if (!UpdateCenter.shouldAutoCheck(context)) return@LaunchedEffect
        kotlinx.coroutines.delay(1500) // даём приложению подняться
        runCheck(manual = false)
    }

    /* ── «Актуальная версия» сама закрывается через 2.2 с —
       но теперь её можно закрыть и руками: крестик, ОК, «назад»,
       тап по затемнению. Никаких «запертых» состояний. ── */
    LaunchedEffect(uiState) {
        if (uiState == UpdateUi.UP_TO_DATE) {
            delay(2200)
            uiState = null
        }
    }

    /* ── Скачивание → авто-установка ── */
    fun startUpdate() {
        val r = release ?: return
        if (!UpdateCenter.canInstallFromApp(context)) {
            uiState = UpdateUi.NEED_PERMISSION
            return
        }
        uiState = UpdateUi.DOWNLOADING
        progress = -1f
        apkFile = null
        pendingInstall = false
        scope.launch {
            val f = UpdateCenter.downloadApk(context, r) { p -> progress = p }
            if (f == null) {
                apkFile = null
                uiState = UpdateUi.FAILED
            } else {
                apkFile = f
                progress = 1f
                tryInstall() // установщик открывается сам
            }
        }
    }

    fun confirmPermission() {
        UpdateCenter.openInstallPermissionSettings(context)
        uiState = UpdateUi.AWAITING_PERMISSION
    }

    /* ── ON_RESUME: вернулись из настроек с разрешением / из фона ── */
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState, pendingInstall) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                when (uiState) {
                    UpdateUi.AWAITING_PERMISSION ->
                        if (UpdateCenter.canInstallFromApp(context)) startUpdate()
                    else ->
                        if (pendingInstall) tryInstall()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /* ── Рендер ── */
    // Системная кнопка «назад» ЗАКРЫВАЕТ карточку обновления — диалог
    // никогда не «запирает» пользователя (это был баг v2.3.2: «обновлений
    // нет», а выйти из карточки нельзя — ни крестика, ни отклика на «назад»).
    BackHandler(enabled = uiState != null) { uiState = null }

    val state = uiState ?: return
    UpdateDialog(
        state = state,
        release = release,
        progress = progress,
        onUpdate = ::startUpdate,
        onLater = {
            release?.let { UpdateCenter.skip(context, it) }
            uiState = null
        },
        onConfirmPermission = ::confirmPermission,
        onRetry = { runCheck(manual = true) },
        onBrowser = {
            release?.let { UpdateCenter.openInBrowser(context, it.apkUrl) }
            uiState = null
        },
        onReleases = {
            UpdateCenter.openReleasesPage(context)
            uiState = null
        },
        onDismiss = { uiState = null }
    )
}

@Composable
private fun UpdateDialog(
    state: UpdateUi,
    release: UpdateCenter.Release?,
    progress: Float,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onConfirmPermission: () -> Unit,
    onRetry: () -> Unit,
    onBrowser: () -> Unit,
    onReleases: () -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            // Тап по затемнению закрывает карточку (как системный «назад»)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Тёмная карточка в стилистике приложения.
            // Поглощает тапы по себе, чтобы не проваливаться в затемнение.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* карточка ест тапы мимо своих кнопок */ }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1C232E),
                                Color(0xFF151B24)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                /* ── Шапка ── */
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(primary, MaterialTheme.colorScheme.tertiary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val headerIcon = when (state) {
                            UpdateUi.UP_TO_DATE -> Icons.Filled.Verified
                            UpdateUi.FAILED -> Icons.Filled.ErrorOutline
                            else -> Icons.Filled.SystemUpdate
                        }
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        val title = when (state) {
                            UpdateUi.UP_TO_DATE -> stringResource(R.string.upd_uptodate)
                            UpdateUi.FAILED -> stringResource(R.string.upd_failed)
                            else -> stringResource(R.string.upd_title)
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        if (release != null && state != UpdateUi.UP_TO_DATE && state != UpdateUi.FAILED) {
                            Text(
                                text = "Pill Tracker ${release.versionName ?: release.tag}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }
                    }

                    // Крестик — всегда в правом верхнем углу: из карточки
                    // можно выйти из ЛЮБОГО состояния (включая «проверяем…»
                    // при зависшей сети).
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.upd_close),
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 6 }) togetherWith
                            fadeOut(tween(160))
                    },
                    label = "updateBody"
                ) { s ->
                    Column {
                        when (s) {
                            UpdateUi.CHECKING -> CheckingBody()
                            UpdateUi.ASKING -> AskingBody(release, onUpdate, onLater)
                            UpdateUi.NEED_PERMISSION -> PermissionBody(
                                onConfirmPermission, onBrowser
                            )
                            UpdateUi.AWAITING_PERMISSION -> AwaitingBody(
                                onConfirmPermission, onBrowser
                            )
                            UpdateUi.DOWNLOADING -> DownloadingBody(
                                progress, onDismiss, onReleases
                            )
                            UpdateUi.UP_TO_DATE -> UpToDateBody(onDismiss)
                            UpdateUi.FAILED -> FailedBody(onRetry, onReleases, onDismiss)
                        }
                    }
                }
            }
        }
    }
}

/* ── Тела состояний ── */

@Composable
private fun CheckingBody() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.upd_checking),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun AskingBody(
    release: UpdateCenter.Release?,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.upd_body, release?.versionName ?: release?.tag ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f)
        )
        if (!release?.noteLines.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.upd_whats_new),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            release.noteLines.forEach { line ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.85f)
                )
            ) {
                Text(stringResource(R.string.upd_later))
            }
            Button(
                onClick = onUpdate,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.upd_now))
            }
        }
    }
}

@Composable
private fun PermissionBody(
    onConfirm: () -> Unit,
    onBrowser: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.upd_perm_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.upd_perm_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(14.dp))
        PermissionSteps()
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = onBrowser,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White.copy(alpha = 0.75f)
            )
        ) {
            Text(stringResource(R.string.upd_browser))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.upd_open_settings))
        }
    }
}

@Composable
private fun AwaitingBody(
    onConfirm: () -> Unit,
    onBrowser: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.upd_waiting),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        PermissionSteps()
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onBrowser,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Text(stringResource(R.string.upd_browser))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.upd_open_settings))
            }
        }
    }
}

@Composable
private fun PermissionSteps() {
    val steps = listOf(
        R.string.upd_perm_step1,
        R.string.upd_perm_step2,
        R.string.upd_perm_step3
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { i, res ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${i + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DownloadingBody(
    progress: Float,
    onDismiss: () -> Unit,
    onReleases: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DownloadDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.upd_downloading),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (progress >= 0) {
            val animated by animateFloatAsState(
                targetValue = progress,
                animationSpec = spring(dampingRatio = 1f, stiffness = 380f),
                label = "downloadProgress"
            )
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.12f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Как это устроено: скрыто, внутри приложения, само ставится
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(14.dp).padding(top = 2.dp)
            )
            Text(
                text = stringResource(R.string.upd_installer_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Text(stringResource(R.string.upd_minimize))
            }
            OutlinedButton(
                onClick = onReleases,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Text(stringResource(R.string.upd_releases))
            }
        }
    }
}

@Composable
private fun UpToDateBody(
    onDismiss: () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = stringResource(R.string.upd_uptodate),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Кнопка «ОК»: карточка закрывается и руками — не только таймером
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(stringResource(R.string.upd_ok))
        }
    }
}

@Composable
private fun FailedBody(
    onRetry: () -> Unit,
    onReleases: () -> Unit,
    onDismiss: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.upd_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Text(stringResource(R.string.upd_close))
            }
            OutlinedButton(
                onClick = onReleases,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.75f)
                )
            ) {
                Text(stringResource(R.string.upd_releases))
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.upd_retry))
            }
        }
    }
}
