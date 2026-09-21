package com.aistudio.meditracker.ui.components

/*
 * UpdateChecker v5 — самообновление целиком внутри приложения.
 *
 * v2.4.7: файл, раздувшийся до 1200+ строк, разложен по полочкам —
 * тот же код, та же логика, но каждый файл держит одну работу:
 *   • UpdateChecker.kt   (этот файл) — шина события + UpdateGate:
 *                          state-machine «когда показаться, что делать»;
 *   • UpdateCenter.kt    — данные и логика: версии, GitHub API,
 *                          скачивание в кеш, чистка, установка;
 *   • UpdateDialog.kt    — каркас карточки (затемнение, шапка, крестик,
 *                          AnimatedContent-переключатель состояний);
 *   • UpdateDialogBodies.kt — тела состояний: «проверяем», «доступно»,
 *                          «разрешение», «качается», «актуально», «ошибка».
 * Публичный API не изменился: UpdateGate() + UpdateBus.requestCheck().
 *
 * Что было в v4/v3 (история сохранена намеренно):
 *  • КРАШ ПРИ ПРОВЕРКЕ ОБНОВЛЕНИЙ устранён (v2.3.2): parseSemver читал
 *    groupValues[1..3] у регулярки БЕЗ групп — теперь совпадение режется
 *    по точкам, а любые исключения глотаются.
 *  • Версия релиза определяется по МАРКЕРУ «app-version: X.Y.Z» в начале
 *    RELEASE_NOTES.md (CI кладёт файл в тело релиза как есть). Это
 *    надёжнее названия релиза и безопаснее парсинга «versionCode…» из
 *    тела. Пересборка ТОЙ ЖЕ версии — НЕ обновление.
 *  • Диалог ВСЕГДА закрывается (v2.4.0): хрестик/«Гаразд»/«назад»/тап
 *    по затемнению работают в любом состоянии.
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

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File

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
 * UpdateGate — оркестратор: когда показать, что делать дальше
 * ──────────────────────────────────────────────────────────────── */

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
