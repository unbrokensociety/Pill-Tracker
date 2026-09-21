package com.aistudio.meditracker.ui.components

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

@Composable
fun UpdateGate() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<UpdateUi?>(null) }
    var release by remember { mutableStateOf<UpdateCenter.Release?>(null) }
    var progress by remember { mutableStateOf(-1f) }
    var apkFile by remember { mutableStateOf<File?>(null) }
    var pendingInstall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UpdateCenter.cleanupDownloads(context)
    }

    fun tryInstall() {
        val f = apkFile ?: return
        if (UpdateCenter.installApk(context, f)) {
            pendingInstall = false
            uiState = null
        } else {
            pendingInstall = true
        }
    }

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
                    uiState = null
                }
            } catch (e: Exception) {
                uiState = if (manual) UpdateUi.FAILED else null
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { UpdateBus.checkRequested }
            .filter { it }
            .collect {
                UpdateBus.consumeCheck()
                runCheck(manual = true)
            }
    }

    LaunchedEffect(Unit) {
        if (!UpdateCenter.shouldAutoCheck(context)) return@LaunchedEffect
        kotlinx.coroutines.delay(1500)
        runCheck(manual = false)
    }

    LaunchedEffect(uiState) {
        if (uiState == UpdateUi.UP_TO_DATE) {
            delay(2200)
            uiState = null
        }
    }

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
                tryInstall()
            }
        }
    }

    fun confirmPermission() {
        UpdateCenter.openInstallPermissionSettings(context)
        uiState = UpdateUi.AWAITING_PERMISSION
    }

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
