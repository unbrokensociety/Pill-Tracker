package com.aistudio.meditracker.ui.components

/*
 * UpdateDialog — каркас карточки обновления (v2.4.7: вынесено из
 * UpdateChecker.kt, код без изменений): затемнение, тёмная карточка,
 * шапка с иконкой и крестиком, AnimatedContent-переключатель тел
 * состояний (сами тела — в UpdateDialogBodies.kt).
 */

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.meditracker.R

/**
 * Состояния карточки обновления. v2.4.7: internal (был private-в-файле),
 * потому что тела состояний теперь живут в UpdateDialogBodies.kt.
 */
internal enum class UpdateUi {
    CHECKING,             // «Проверяем…»
    ASKING,               // «Доступно обновление» + кнопки
    NEED_PERMISSION,      // инструкция «разреши один раз»
    AWAITING_PERMISSION,  // ушли в настройки, ждём возвращения
    DOWNLOADING,
    UP_TO_DATE,
    FAILED
}

@Composable
internal fun UpdateDialog(
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
