package com.example.ui.components

/*
 * UpdateChecker — обновление приложения прямо из приложения.
 *
 * Поток:
 *  1. При запуске (не чаще, чем раз в 3 часа) и по кнопке в Настройках
 *     спрашиваем GitHub API: /releases/latest.
 *  2. CI пишет в тело релиза строку «versionCode NNNN» — по ней мы
 *     железно сравниваем: свежее ли это, чем установлено.
 *  3. Если свежее — карточка «Доступно обновление» с заметками.
 *  4. «Обновить»: если Android ещё не разрешал установку из приложения —
 *     показываем инструкцию (3 шага) и открываем системные настройки
 *     (ACTION_MANAGE_UNKNOWN_APP_SOURCES). Разрешил и вернулся —
 *     скачивание продолжается само (lifecycle ON_RESUME).
 *  5. APK качает системный DownloadManager (с прогрессом и уведомлением),
 *     по завершении автоматически открывается пакетный установщик.
 *  6. Fallback: «Скачать через браузер» — открываем ссылку APK.
 *
 * Никаких сторонних библиотек: HttpURLConnection + org.json (встроен в
 * Android) + DownloadManager + FileProvider (уже объявлен в манифесте).
 */

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import kotlinx.coroutines.Dispatchers
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
    private const val KEY_SKIPPED_CODE = "skipped_version_code"
    private const val KEY_LAST_CHECK = "last_check_ms"

    /** Проверяем автоматически не чаще, чем раз в 3 часа. */
    private const val AUTO_CHECK_INTERVAL_MS = 3L * 60 * 60 * 1000

    private const val APK_FILE_NAME = "pill-tracker-update.apk"

    data class Release(
        val tag: String,            // «v1.87» — для показа пользователю
        val versionCode: Long,      // из тела релиза (пишет CI); 0 = нет маркера
        val apkUrl: String,         // прямой URL .apk из assets
        val pageUrl: String,        // страница релиза
        val noteLines: List<String> // короткий список «что нового»
    )

    data class DownloadStatus(val status: Int, val soFar: Long, val total: Long)

    /* ── версия установленного приложения ── */

    fun installedVersionCode(context: android.content.Context): Long {
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info)
    }

    /* ── SharedPreferences: троттлинг и «позже» ── */

    private fun prefs(context: android.content.Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun shouldAutoCheck(context: android.content.Context): Boolean {
        val last = prefs(context).getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last > AUTO_CHECK_INTERVAL_MS
    }

    fun markChecked(context: android.content.Context) {
        prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }

    /** Эту версию уже откладывали «Позже»? */
    fun isSkipped(context: android.content.Context, release: Release): Boolean {
        if (release.versionCode <= 0) return false
        return prefs(context).getLong(KEY_SKIPPED_CODE, -1L) >= release.versionCode
    }

    fun skip(context: android.content.Context, release: Release) {
        if (release.versionCode > 0) {
            prefs(context).edit().putLong(KEY_SKIPPED_CODE, release.versionCode).apply()
        }
    }

    /**
     * Релиз свежее установленного? Если CI-маркер versionCode не найден
     * (старый формат тела релиза) — считаем «возможно свежее» и показываем
     * карточку: решение всё равно принимает пользователь.
     */
    fun isNewerThanInstalled(context: android.content.Context, release: Release): Boolean {
        if (release.versionCode <= 0) return true
        return release.versionCode > installedVersionCode(context)
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

        // CI пишет в тело: «🔧 Build info: versionCode 3087 · cert …»
        val code = Regex("versionCode\\D{0,12}(\\d{1,9})")
            .find(rawNotes)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L

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

        // Заметки: строки до разделителя «---» (тех. подпись CI не показываем),
        // максимум 4 строки, без markdown-мусора.
        val notes = rawNotes
            .substringBefore("\n---")
            .lineSequence()
            .map { it.trim() }
            .map { it.removePrefix("- ").removePrefix("* ").removePrefix("• ") }
            .map { it.replace("**", "").replace("###", "").trim() }
            .filter { it.isNotBlank() && it.length > 2 }
            .take(4)
            .toList()

        return Release(tag, code, apkUrl, pageUrl, notes)
    }

    /* ── разрешение на установку (один раз, Android 8+) ── */

    fun canInstallFromApp(context: android.content.Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: android.content.Context) {
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

    /* ── скачивание через системный DownloadManager ── */

    fun downloadApk(context: android.content.Context, release: Release): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(release.apkUrl))
            .setTitle("Pill Tracker ${release.tag}")
            .setDescription(context.getString(R.string.upd_notif_desc))
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationInExternalFilesDir(
                context, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME
            )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        return dm.enqueue(request)
    }

    fun downloadStatus(context: android.content.Context, id: Long): DownloadStatus? {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(id)
        dm.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val soFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                return DownloadStatus(status, soFar, total)
            }
        }
        return null
    }

    /* ── установка ── */

    fun installApk(context: android.content.Context): Boolean {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
            val apk = File(dir, APK_FILE_NAME)
            if (!apk.exists() || apk.length() < 10_000) return false
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

    fun openReleasesPage(context: android.content.Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openInBrowser(context: android.content.Context, url: String) {
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
    var downloadId by remember { mutableStateOf<Long?>(null) }
    var progress by remember { mutableStateOf(-1f) } // -1 = неизвестно

    /* ── Ручная проверка из Настроек ── */
    LaunchedEffect(Unit) {
        snapshotFlow { UpdateBus.checkRequested }
            .filter { it }
            .collect {
                UpdateBus.consumeCheck()
                uiState = UpdateUi.CHECKING
                UpdateCenter.markChecked(context)
                val r = UpdateCenter.fetchLatest()
                if (r == null) {
                    uiState = UpdateUi.FAILED
                } else if (UpdateCenter.isNewerThanInstalled(context, r) &&
                    !UpdateCenter.isSkipped(context, r)
                ) {
                    release = r
                    uiState = UpdateUi.ASKING
                } else {
                    uiState = UpdateUi.UP_TO_DATE
                }
            }
    }

    /* ── Автопроверка при запуске (не чаще раза в 3 часа) ── */
    LaunchedEffect(Unit) {
        if (!UpdateCenter.shouldAutoCheck(context)) return@LaunchedEffect
        kotlinx.coroutines.delay(1500) // даём приложению подняться
        val r = UpdateCenter.fetchLatest()
        UpdateCenter.markChecked(context)
        if (r != null &&
            UpdateCenter.isNewerThanInstalled(context, r) &&
            !UpdateCenter.isSkipped(context, r)
        ) {
            release = r
            uiState = UpdateUi.ASKING
        }
    }

    /* ── Прогресс скачивания (поллинг DownloadManager) ── */
    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        progress = -1f
        while (isActive) {
            val s = UpdateCenter.downloadStatus(context, id)
            if (s != null) {
                when {
                    s.status == DownloadManager.STATUS_SUCCESSFUL -> {
                        progress = 1f
                        return@LaunchedEffect
                    }
                    s.status == DownloadManager.STATUS_FAILED -> {
                        downloadId = null
                        uiState = UpdateUi.FAILED
                        return@LaunchedEffect
                    }
                    else -> if (s.total > 0) {
                        progress = s.soFar.toFloat() / s.total
                    }
                }
            }
            delay(700)
        }
    }

    /* ── Скачивание завершилось → открываем установщик ── */
    fun onDownloadComplete(id: Long) {
        if (downloadId != null && id == downloadId) {
            progress = 1f
            if (UpdateCenter.installApk(context)) {
                uiState = null // установщик открыт
            } else {
                uiState = UpdateUi.FAILED
            }
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id >= 0) onDownloadComplete(id)
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    /* ── Вернулся из системных настроек с разрешением → качаем сами ── */
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, uiState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                uiState == UpdateUi.AWAITING_PERMISSION &&
                UpdateCenter.canInstallFromApp(context)
            ) {
                val r = release
                if (r != null) {
                    downloadId = UpdateCenter.downloadApk(context, r)
                    uiState = UpdateUi.DOWNLOADING
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /* ── «Актуальная версия» сама закрывается через 2.5 с ── */
    LaunchedEffect(uiState) {
        if (uiState == UpdateUi.UP_TO_DATE) {
            delay(2500)
            uiState = null
        }
    }

    /* ── Действия ── */
    fun startUpdate() {
        val r = release ?: return
        if (UpdateCenter.canInstallFromApp(context)) {
            downloadId = UpdateCenter.downloadApk(context, r)
            uiState = UpdateUi.DOWNLOADING
        } else {
            uiState = UpdateUi.NEED_PERMISSION
        }
    }

    fun confirmPermission() {
        UpdateCenter.openInstallPermissionSettings(context)
        uiState = UpdateUi.AWAITING_PERMISSION
    }

    fun checkAgain() {
        uiState = UpdateUi.CHECKING
        scope.launch {
            UpdateCenter.markChecked(context)
            val r = UpdateCenter.fetchLatest()
            if (r == null) {
                uiState = UpdateUi.FAILED
            } else if (UpdateCenter.isNewerThanInstalled(context, r) &&
                !UpdateCenter.isSkipped(context, r)
            ) {
                release = r
                uiState = UpdateUi.ASKING
            } else {
                uiState = UpdateUi.UP_TO_DATE
            }
        }
    }

    /* ── Рендер ── */
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
        onRetry = ::checkAgain,
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
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Тёмная карточка в стилистике приложения
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                                text = "Pill Tracker ${release.tag}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                        }
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
                            UpdateUi.UP_TO_DATE -> UpToDateBody()
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
            text = stringResource(R.string.upd_body, release?.tag ?: ""),
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
        val animated by animateFloatAsState(
            targetValue = if (progress < 0) 0f else progress,
            animationSpec = spring(dampingRatio = 1f, stiffness = 380f),
            label = "downloadProgress"
        )
        LinearProgressIndicator(
            progress = {
                if (progress < 0) return@LinearProgressIndicator 0f
                animated
            },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.12f)
        )
        if (progress >= 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.upd_notif_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f)
        )
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
private fun UpToDateBody() {
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
